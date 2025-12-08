package cl.camodev.wosbot.api.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dispatches incoming messages to appropriate handlers.
 * Full implementation in step 1.3.
 */
public class MessageDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(MessageDispatcher.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String handleMessage(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);

            // For now, just echo back
            logger.info("Handling message: {}", message);
            return "{\"success\":true,\"message\":\"Echo: " + message.replace("\"", "\\\"") + "\"}";

        } catch (Exception e) {
            logger.error("Failed to parse message", e);
            return "{\"success\":false,\"error\":\"Invalid JSON\"}";
        }
    }
}