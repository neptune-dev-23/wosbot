package cl.camodev.wosbot.web.streaming;

import cl.camodev.wosbot.console.list.ILogListener;
import cl.camodev.wosbot.ot.DTOLogMessage;
import cl.camodev.wosbot.web.websocket.WebSocketMessagingService;
import cl.camodev.wosbot.web.websocket.WebSocketTopicListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Service responsible for streaming logs to web clients via WebSockets.
 */
@Service
public class LogStreamingService implements ILogListener, WebSocketTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(LogStreamingService.class);
    private static final int MAX_LOG_HISTORY = 1000;

    private final Queue<DTOLogMessage> logHistory = new ConcurrentLinkedQueue<>();
    private final WebSocketMessagingService messagingService;

    public LogStreamingService(WebSocketMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public String topic() {
        return "logs";
    }

    @Override
    public void onSubscribe(WebSocketSession session) {
        var snapshot = logHistory.stream().collect(Collectors.toList());
        messagingService.send(session, "logs.snapshot", snapshot);
    }

    @Override
    public void onLogReceived(DTOLogMessage message) {
        logger.debug("Received log: {} - {}", message.getSeverity(), message.getMessage());
        
        // Add to history (with size limit)
        logHistory.offer(message);
        while (logHistory.size() > MAX_LOG_HISTORY) {
            logHistory.poll();
        }

        // Broadcast to all connected clients
        messagingService.broadcast("logs", "logs.append", message);
    }
}
