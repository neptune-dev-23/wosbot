package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.mockito.Mockito.*;

class WebSocketHandlerTest {

    private WebSocketHandler handler;
    private WebSocketSessionManager sessionManager;
    private MessageDispatcher messageDispatcher;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        sessionManager = mock(WebSocketSessionManager.class);
        messageDispatcher = mock(MessageDispatcher.class);
        handler = new WebSocketHandler(sessionManager, messageDispatcher);
        session = mock(WebSocketSession.class);
    }

    @Test
    void testAfterConnectionEstablished() throws Exception {
        handler.afterConnectionEstablished(session);
        verify(sessionManager).addSession(session);
    }

    @Test
    void testAfterConnectionClosed() throws Exception {
        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        verify(sessionManager).removeSession(session);
    }

    @Test
    void testHandleTextMessage() throws Exception {
        String payload = "{\"test\":\"data\"}";
        TextMessage message = new TextMessage(payload);
        when(messageDispatcher.handleMessage(payload)).thenReturn("response");

        handler.handleTextMessage(session, message);

        verify(messageDispatcher).handleMessage(payload);
        verify(sessionManager).sendToSession(session, "response");
    }
}
