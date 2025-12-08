package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WebSocketHandlerTest {
    private WebSocketSessionManager sessionManager;
    private MessageDispatcher dispatcher;
    private WebSocketHandler handler;
    private WebSocketSession session;

    @BeforeEach
    void setUp() {
        sessionManager = mock(WebSocketSessionManager.class);
        dispatcher = mock(MessageDispatcher.class);
        handler = new WebSocketHandler(sessionManager, dispatcher);

        session = mock(WebSocketSession.class);
        when(session.getRemoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 12345));
    }

    @Test
    void registersAndUnregistersSessions() throws Exception {
        handler.afterConnectionEstablished(session);
        verify(sessionManager).addSession(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);
        verify(sessionManager).removeSession(session);
    }

    @Test
    void dispatchesIncomingTextMessages() throws Exception {
        TextMessage message = new TextMessage("{\"command\":\"test\"}");
        when(dispatcher.handleMessage(message.getPayload())).thenReturn("response");

        handler.handleTextMessage(session, message);

        verify(dispatcher).handleMessage(message.getPayload());
        verify(sessionManager).sendToSession(session, "response");
    }

    @Test
    void repliesWithErrorWhenDispatcherFails() throws Exception {
        TextMessage message = new TextMessage("{\"command\":\"broken\"}");
        when(dispatcher.handleMessage(anyString())).thenThrow(new RuntimeException("boom"));

        handler.handleTextMessage(session, message);

        verify(sessionManager).sendToSession(eq(session),
                argThat(text -> text.contains("\"success\":false") && text.contains("Internal error")));
    }
}
