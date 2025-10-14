package cl.camodev.wosbot.web.websocket;

import cl.camodev.wosbot.web.config.JsonSerializerConfig;
import com.google.gson.Gson;
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

/**
 * Central WebSocket messaging hub that keeps track of connected sessions and their topic subscriptions.
 */
@Component
public class WebSocketMessagingService {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketMessagingService.class);
    private static final String WILDCARD_TOPIC = "*";

    private final Gson gson = JsonSerializerConfig.getGson();
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final Map<WebSocketSession, Set<String>> subscriptions = new ConcurrentHashMap<>();

    public void registerSession(WebSocketSession session) {
        sessions.add(session);
        subscriptions.put(session, ConcurrentHashMap.newKeySet());
    }

    public void unregisterSession(WebSocketSession session) {
        sessions.remove(session);
        subscriptions.remove(session);
    }

    public void subscribe(WebSocketSession session, Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return;
        }
        Set<String> sessionTopics = subscriptions.computeIfAbsent(session, key -> ConcurrentHashMap.newKeySet());
        sessionTopics.addAll(topics);
        logger.debug("Session {} subscribed to topics {}", session.getId(), sessionTopics);
    }

    public void unsubscribe(WebSocketSession session, Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            return;
        }
        Set<String> sessionTopics = subscriptions.get(session);
        if (sessionTopics != null) {
            sessionTopics.removeAll(topics);
            logger.debug("Session {} remaining topics {}", session.getId(), sessionTopics);
        }
    }

    public Set<String> getSubscriptions(WebSocketSession session) {
        return subscriptions.getOrDefault(session, Collections.emptySet());
    }

    public void broadcast(String topic, String type, Object payload) {
        for (WebSocketSession session : sessions) {
            if (isSubscribed(session, topic)) {
                send(session, type, payload);
            }
        }
    }

    public void send(WebSocketSession session, String type, Object payload) {
        if (!session.isOpen()) {
            return;
        }
        WebSocketMessage message = new WebSocketMessage(type, payload);
        String json = gson.toJson(message);
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(json));
            }
        } catch (IOException ex) {
            logger.warn("Failed to send WebSocket message to session {}: {}", session.getId(), ex.getMessage());
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
}
