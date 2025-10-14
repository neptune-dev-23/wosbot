package cl.camodev.wosbot.web.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AppWebSocketHandler appWebSocketHandler;

    public WebSocketConfig(AppWebSocketHandler appWebSocketHandler) {
        this.appWebSocketHandler = appWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(appWebSocketHandler, "/ws")
                .setAllowedOriginPatterns("*");
    }
}
