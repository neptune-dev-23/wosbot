package cl.camodev.wosbot.web.websocket;

import cl.camodev.wosbot.web.config.JsonSerializerConfig;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Primary WebSocket handler that coordinates client subscriptions and routes messages.
 */
@Component
public class AppWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(AppWebSocketHandler.class);
    private static final String TYPE_SUBSCRIBE = "subscribe";
    private static final String TYPE_UNSUBSCRIBE = "unsubscribe";
    private static final String TYPE_PING = "ping";

    private final WebSocketMessagingService messagingService;
    private final Map<String, WebSocketTopicListener> topicListeners;
    private final Gson gson = JsonSerializerConfig.getGson();

    public AppWebSocketHandler(
            WebSocketMessagingService messagingService,
            List<WebSocketTopicListener> topicListeners
    ) {
        this.messagingService = messagingService;
        this.topicListeners = topicListeners.stream()
                .collect(Collectors.toUnmodifiableMap(WebSocketTopicListener::topic, listener -> listener, (left, right) -> right));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        messagingService.registerSession(session);
        logger.info("WebSocket session {} connected", session.getId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("message", "connected");
        messagingService.send(session, "system.connected", payload);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            InboundMessage inbound = gson.fromJson(message.getPayload(), InboundMessage.class);
            if (inbound == null || inbound.type == null) {
                sendError(session, "invalid.message", "Missing message type");
                return;
            }
            switch (inbound.type) {
                case TYPE_SUBSCRIBE -> handleSubscribe(session, inbound.topics);
                case TYPE_UNSUBSCRIBE -> handleUnsubscribe(session, inbound.topics);
                case TYPE_PING -> messagingService.send(session, "system.pong", Collections.singletonMap("ts", System.currentTimeMillis()));
                default -> sendError(session, "unsupported.message", "Unsupported message type: " + inbound.type);
            }
        } catch (Exception ex) {
            logger.warn("Invalid WebSocket message from session {}: {}", session.getId(), ex.getMessage());
            sendError(session, "invalid.message", ex.getMessage());
        }
    }

    private void handleSubscribe(WebSocketSession session, Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            sendError(session, "subscribe.missingTopics", "No topics provided for subscription");
            return;
        }
        messagingService.subscribe(session, topics);
        messagingService.send(session, "system.subscribed", Collections.singletonMap("topics", topics));

        for (String topic : topics) {
            WebSocketTopicListener listener = topicListeners.get(topic);
            if (listener != null) {
                try {
                    listener.onSubscribe(session);
                } catch (Exception ex) {
                    logger.error("Topic listener error for {}: {}", topic, ex.getMessage());
                    sendError(session, "listener.error", ex.getMessage());
                }
            } else {
                logger.debug("No topic listener registered for {}", topic);
            }
        }
    }

    private void handleUnsubscribe(WebSocketSession session, Collection<String> topics) {
        if (topics == null || topics.isEmpty()) {
            sendError(session, "unsubscribe.missingTopics", "No topics provided for unsubscribe");
            return;
        }
        messagingService.unsubscribe(session, topics);
        messagingService.send(session, "system.unsubscribed", Collections.singletonMap("topics", topics));
    }

    private void sendError(WebSocketSession session, String code, String detail) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("detail", detail);
        messagingService.send(session, "system.error", payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        messagingService.unregisterSession(session);
        logger.info("WebSocket session {} disconnected: {}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.warn("Transport error on session {}: {}", session.getId(), exception.getMessage());
        messagingService.unregisterSession(session);
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static class InboundMessage {
        String type;
        Set<String> topics;
    }
}
