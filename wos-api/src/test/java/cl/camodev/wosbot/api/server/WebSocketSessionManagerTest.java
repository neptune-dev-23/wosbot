package cl.camodev.wosbot.api.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class WebSocketSessionManagerTest {
    private WebSocketSessionManager manager;

    @BeforeEach
    void setUp() {
        manager = new WebSocketSessionManager();
    }

    @Test
    void addsAndRemovesSessions() {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-1");

        manager.addSession(session);
        assertEquals(1, manager.getActiveConnectionCount());

        manager.removeSession(session);
        assertEquals(0, manager.getActiveConnectionCount());
    }

    @Test
    void sendsMessagesOnlyToOpenSessions() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("session-open");
        when(session.isOpen()).thenReturn(true);
        when(session.getRemote()).thenReturn(mock(org.springframework.web.socket.WebSocketMessage.class));
        manager.addSession(session);
        manager.sendToSession(session, "hello");

        verify(session).sendMessage(argThat(message -> "hello".equals(message.getPayload())));
    }

    @Test
    void broadcastSkipsClosedSessions() throws Exception {
        WebSocketSession openSession = mock(WebSocketSession.class);
        when(openSession.getId()).thenReturn("open");
        when(openSession.isOpen()).thenReturn(true);

        WebSocketSession closedSession = mock(WebSocketSession.class);
        when(closedSession.getId()).thenReturn("closed");
        when(closedSession.isOpen()).thenReturn(false);

        manager.addSession(openSession);
        manager.addSession(closedSession);

        manager.broadcast("payload");

        verify(openSession).sendMessage(argThat(message -> "payload".equals(message.getPayload())));
        verify(closedSession, never()).sendMessage(any());
    }

    @Test
    void closeAllClearsSessions() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("close");

        manager.addSession(session);
        manager.closeAll();

        assertEquals(0, manager.getActiveConnectionCount());
        verify(session).close();
    }
}
