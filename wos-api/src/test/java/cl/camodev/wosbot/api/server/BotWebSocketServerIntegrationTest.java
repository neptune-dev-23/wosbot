package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BotWebSocketServerIntegrationTest {
    @LocalServerPort
    private int port;
    private final StandardWebSocketClient client = new StandardWebSocketClient();

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterEach
    void tearDown() {
        client.stop();
    }

    @Test
    void websocketHandlerEchoesMessages() throws Exception {
        TestSocketHandler handler = new TestSocketHandler();
        client.setMessageConverter(new StringMessageConverter());

        WebSocketSession session = handler.awaitConnection(client.doHandshake(handler, "ws://localhost:" + port + "/bot"));

        String payload = "{\"command\":\"test\"}";
        session.sendMessage(new TextMessage(payload));

        String response = handler.awaitMessage();
        assertTrue(response.contains("\"success\":true"));
        assertTrue(response.contains("Echo: " + payload));

        session.close(CloseStatus.NORMAL);
    }

    @Test
    void healthEndpointReportsUp() {
        ResponseEntity<Map> response = restTemplate.getForEntity("http://localhost:" + port + "/health", Map.class);
        assertEquals("UP", response.getBody().get("status"));
        assertEquals(200, response.getStatusCodeValue());
    }

    private static class TestSocketHandler extends AbstractWebSocketHandler {
        private final CompletableFuture<WebSocketSession> sessionFuture = new CompletableFuture<>();
        private final CompletableFuture<String> messageFuture = new CompletableFuture<>();

        WebSocketSession awaitConnection(ListenableFuture<WebSocketSession> handshake) throws Exception {
            return handshake.get(5, TimeUnit.SECONDS);
        }

        String awaitMessage() throws Exception {
            return messageFuture.get(5, TimeUnit.SECONDS);
        }

        @Override
        protected void handleTextMessage(WebSocketSession session, TextMessage message) {
            messageFuture.complete(message.getPayload());
        }
    }
}
