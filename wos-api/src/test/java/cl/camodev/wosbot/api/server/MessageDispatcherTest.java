package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageDispatcherTest {
    private final MessageDispatcher dispatcher = new MessageDispatcher();

    @Test
    void echoesValidJsonMessages() {
        String payload = "{\"command\":\"status\"}";
        String response = dispatcher.handleMessage(payload);

        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("Echo: " + payload));
    }

    @Test
    void reportsFailureForInvalidJson() {
        String response = dispatcher.handleMessage("not-json");

        assertTrue(response.contains("\"success\":false"));
        assertTrue(response.contains("Invalid JSON"));
    }
}
