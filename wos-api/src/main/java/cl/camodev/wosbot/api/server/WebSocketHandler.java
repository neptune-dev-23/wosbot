package cl.camodev.wosbot.api.server;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles WebSocket connections and messages.
 */
public class WebSocketHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);

    private final WebSocketSessionManager sessionManager;
    private final MessageDispatcher messageDispatcher;

    public WebSocketHandler(WebSocketSessionManager sessionManager,
                           MessageDispatcher messageDispatcher) {
        this.sessionManager = sessionManager;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        logger.info("WebSocket connection opened: {}", session.getRemoteAddress());
        sessionManager.addSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        logger.info("WebSocket connection closed: {}. Status: {}, Reason: {}",
                   session.getRemoteAddress(), status.getCode(), status.getReason());
        sessionManager.removeSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        logger.debug("Received message from {}: {}", session.getRemoteAddress(), payload);

        try {
            String response = messageDispatcher.handleMessage(payload);
            sessionManager.sendToSession(session, response);
        } catch (Exception e) {
            logger.error("Error handling message", e);
            sendError(session, "Internal error: " + e.getMessage());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        logger.error("WebSocket error for session {}", session.getRemoteAddress(), exception);
    }

    private void sendError(WebSocketSession session, String errorMessage) {
        String errorJson = String.format(
            "{\"success\":false,\"error\":\"%s\"}",
            errorMessage.replace("\"", "\\\"")
        );
        sessionManager.sendToSession(session, errorJson);
    }
}
