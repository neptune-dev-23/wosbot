package cl.camodev.wosbot.web.websocket;

/**
 * Simple DTO representing an outbound WebSocket message.
 *
 * @param type    semantic message type (e.g. "logs.append")
 * @param payload serialized payload routed to subscribers
 */
public record WebSocketMessage(String type, Object payload) {
}
