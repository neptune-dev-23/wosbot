package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class WebSocketSessionManagerTest {

    private WebSocketSessionManager sessionManager;
    private WebSocketSession session1;
    private WebSocketSession session2;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager();
        session1 = mock(WebSocketSession.class);
        session2 = mock(WebSocketSession.class);

        when(session1.getId()).thenReturn("s1");
        when(session2.getId()).thenReturn("s2");
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);
    }

    @Test
    void testAddAndRemoveSession() {
        sessionManager.addSession(session1);
        assertEquals(1, sessionManager.getActiveConnectionCount());

        sessionManager.addSession(session2);
        assertEquals(2, sessionManager.getActiveConnectionCount());

        sessionManager.removeSession(session1);
        assertEquals(1, sessionManager.getActiveConnectionCount());
    }

    @Test
    void testSendToSession() throws Exception {
        sessionManager.addSession(session1);
        sessionManager.sendToSession(session1, "hello");

        verify(session1).sendMessage(any(TextMessage.class));
    }

    @Test
    void testBroadcast() throws Exception {
        sessionManager.addSession(session1);
        sessionManager.addSession(session2);

        sessionManager.broadcast("broadcast");

        verify(session1).sendMessage(any(TextMessage.class));
        verify(session2).sendMessage(any(TextMessage.class));
    }
}
