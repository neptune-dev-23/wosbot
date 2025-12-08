package cl.camodev.wosbot.api.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

import java.util.Map;

/**
 * Main WebSocket server for Bot API using Spring Boot.
 */
@SpringBootApplication
@EnableWebSocket
public class BotWebSocketServer implements WebSocketConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(BotWebSocketServer.class);
    private final WebSocketSessionManager sessionManager;
    private final MessageDispatcher messageDispatcher;

    public BotWebSocketServer() {
        this.sessionManager = new WebSocketSessionManager();
        this.messageDispatcher = new MessageDispatcher();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        logger.info("Configuring WebSocket endpoints");
        registry.addHandler(new WebSocketHandler(sessionManager, messageDispatcher), "/bot")
                .setAllowedOrigins("*");
    }

    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    public WebSocketSessionManager getSessionManager() {
        return sessionManager;
    }

    public static void main(String[] args) {
        logger.info("Starting Spring Boot WebSocket server...");
        SpringApplication.run(BotWebSocketServer.class, args);
    }

    @RestController
    private static class HealthController {
        @GetMapping("/health")
        public Map<String, String> health() {
            return Map.of("status", "UP");
        }
    }
}
