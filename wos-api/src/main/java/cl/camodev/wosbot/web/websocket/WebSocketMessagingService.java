package cl.camodev.wosbot.web.websocket;

import cl.camodev.wosbot.logging.WebLogger;
import cl.camodev.wosbot.web.config.JsonSerializerConfig;
import com.google.gson.Gson;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

/**
 * Central WebSocket messaging hub that keeps track of connected sessions and their topic subscriptions.
 */
@Component
public class WebSocketMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessagingService.class);
    private static final String WILDCARD_TOPIC = "*";

    private final WebLogger webLogger = new WebLogger(WebSocketMessagingService.class);
    private final Gson gson = JsonSerializerConfig.getGson();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<WebSocketSession, Set<String>> subscriptions = new ConcurrentHashMap<>();
    private final ExecutorService outboundExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "web-ui-dispatcher");
        thread.setDaemon(true);
        return thread;
    });

    public void registerSession(WebSocketSession session) {
        sessions.add(session);
        subscriptions.put(session, ConcurrentHashMap.newKeySet());
        webLogger.info("WebSocket session " + session.getId() + " connected from " + session.getRemoteAddress());
        try {
            session.setTextMessageSizeLimit(256 * 1024);
        } catch (IllegalStateException ignored) {
            // Some session implementations may not support resizing; ignore.
        }
    }

    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
        subscriptions.remove(session);
        webLogger.info("WebSocket session " + session.getId() + " disconnected");
    }

    public void subscribe(WebSocketSession session, Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return;
        }
        Set<String> sessionTopics = subscriptions.computeIfAbsent(session, key -> ConcurrentHashMap.newKeySet());
        sessionTopics.addAll(topics);
        logger.debug("Session {} subscribed to topics {}", session.getId(), sessionTopics);
        webLogger.debug("WebSocket session " + session.getId() + " subscribed to " + sessionTopics);
    }

    public void unsubscribe(WebSocketSession session, Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return;
        }
        Set<String> sessionTopics = subscriptions.get(session);
        if (sessionTopics != null) {
            sessionTopics.removeAll(topics);
            logger.debug("Session {} remaining topics {}", session.getId(), sessionTopics);
            webLogger.debug("WebSocket session " + session.getId() + " remaining topics " + sessionTopics);
        }
    }

    public Set<String> getSubscriptions(WebSocketSession session) {
        return subscriptions.getOrDefault(session, Collections.emptySet());
    }

    public void broadcast(String topic, String type, Object payload) {
        int delivered = 0;
        for (WebSocketSession session : sessions) {
            if (isSubscribed(session, topic)) {
                enqueueSend(session, type, payload);
                delivered++;
            }
        }
        webLogger.debug("Broadcast " + type + " on topic '" + topic + "' to " + delivered + " sessions");
    }

    public void send(WebSocketSession session, String type, Object payload) {
        enqueueSend(session, type, payload);
    }

    private void enqueueSend(WebSocketSession session, String type, Object payload) {
        try {
            outboundExecutor.execute(() -> sendInternal(session, type, payload));
        } catch (RejectedExecutionException ex) {
            logger.warn("Web UI dispatcher rejected message for session {}: {}", session.getId(), ex.getMessage());
            webLogger.warn("Dispatcher rejected message for session " + session.getId() + ": " + ex.getMessage());
        }
    }

    private void sendInternal(WebSocketSession session, String type, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        WebSocketMessage message = new WebSocketMessage(type, payload);
        String json = gson.toJson(message);
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IllegalStateException | IOException ex) {
            logger.warn("Failed to send WebSocket message to session {}: {}", session.getId(), ex.getMessage());
            webLogger.warn("Failed to send WebSocket message to session " + session.getId() + ": " + ex.getMessage());
            safeClose(session);
        }
    }

    private boolean isSubscribed(WebSocketSession session, String topic) {
        Set<String> sessionTopics = subscriptions.get(session);
        if (sessionTopics == null || sessionTopics.isEmpty()) {
            return false;
        }
        return sessionTopics.contains(topic) || sessionTopics.contains(WILDCARD_TOPIC);
    }

    private void safeClose(WebSocketSession session) {
        try {
            session.close(CloseStatus.PROTOCOL_ERROR);
        } catch (IOException ignored) {
            // Ignore close failures
        } finally {
            unregisterSession(session);
        }
    }

    @PreDestroy
    public void shutdown() {
        outboundExecutor.shutdownNow();
    }
}
