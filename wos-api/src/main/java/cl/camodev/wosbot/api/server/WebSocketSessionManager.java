package cl.camodev.wosbot.api.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions (connections) and provides broadcast
 * functionality.
 */
public class WebSocketSessionManager {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionManager.class);

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * Register a new session when client connects.
     */
    public void addSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        logger.info("Client connected. Session ID: {}. Total clients: {}",
                sessionId, sessions.size());
    }

    /**
     * Remove session when client disconnects.
     */
    public void removeSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        logger.info("Client disconnected. Session ID: {}. Total clients: {}",
                sessionId, sessions.size());
    }

    /**
     * Send message to specific session.
     */
    public void sendToSession(WebSocketSession session, String message) {
        try {
            if (session.isOpen()) {
                session.sendMessage(new org.springframework.web.socket.TextMessage(message));
            } else {
                logger.warn("Attempted to send to closed session: {}", session);
            }
        } catch (Exception e) {
            logger.error("Failed to send message to session: {}", session, e);
        }
    }

    /**
     * Broadcast message to all connected clients.
     */
    public void broadcast(String message) {
        int successCount = 0;
        int failCount = 0;

        for (WebSocketSession session : sessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new org.springframework.web.socket.TextMessage(message));
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to broadcast to session: {}", session, e);
                failCount++;
            }
        }

        logger.debug("Broadcast complete. Success: {}, Failed: {}", successCount, failCount);
    }

    /**
     * Get count of active connections.
     */
    public int getActiveConnectionCount() {
        return sessions.size();
    }

    /**
     * Close all sessions (for shutdown).
     */
    public void closeAll() {
        logger.info("Closing all {} sessions", sessions.size());
        sessions.values().forEach(session -> {
            try {
                session.close();
            } catch (Exception e) {
                logger.error("Error closing session", e);
            }
        });
        sessions.clear();
    }
}
