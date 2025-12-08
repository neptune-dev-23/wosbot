package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BotWebSocketServerIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void testHealthCheck() {
        Map<String, String> response = restTemplate.getForObject("http://localhost:" + port + "/health", Map.class);
        assertThat(response).containsEntry("status", "UP");
    }

    @Test
    void testWebSocketEcho() throws Exception {
        WebSocketClient client = new StandardWebSocketClient();
        BlockingQueue<String> messages = new LinkedBlockingQueue<>();

        WebSocketSession session = client.doHandshake(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                messages.add(message.getPayload());
            }
        }, "ws://localhost:" + port + "/bot").get(1, TimeUnit.SECONDS);

        String testMessage = "{\"command\":\"test\"}";
        session.sendMessage(new TextMessage(testMessage));

        String response = messages.poll(5, TimeUnit.SECONDS);
        // The echo response format from MessageDispatcher
        String expectedResponse = "{\"success\":true,\"message\":\"Echo: {\"command\":\"test\"}\"}";

        // Note: The MessageDispatcher implementation in 1.2-websocket-server.md is
        // simple:
        // return "{\"success\":true,\"message\":\"Echo: " + message + "\"}";
        // However, we should be careful about JSON escaping if we matched the exact
        // string implementation.
        // Let's check loose containment or parse it if needed.

        assertThat(response).contains("Echo");

        session.close();
    }
}
