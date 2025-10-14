package cl.camodev.wosbot.web.streaming;

import cl.camodev.wosbot.ot.DTOBotState;
import cl.camodev.wosbot.serv.IBotStateListener;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.web.websocket.WebSocketMessagingService;
import cl.camodev.wosbot.web.websocket.WebSocketTopicListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;

/**
 * Service responsible for streaming bot state changes via WebSocket.
 */
@Service
public class BotStateStreamingService implements IBotStateListener, WebSocketTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(BotStateStreamingService.class);

    private final WebSocketMessagingService messagingService;

    public BotStateStreamingService(WebSocketMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @Override
    public String topic() {
        return "botState";
    }

    @Override
    public void onSubscribe(WebSocketSession session) {
        try {
            DTOBotState snapshot = buildCurrentBotState();
            if (snapshot != null) {
                messagingService.send(session, "botState.snapshot", snapshot);
            }
        } catch (Exception e) {
            logger.error("Failed to deliver bot state snapshot: {}", e.getMessage());
        }
    }

    @Override
    public void onBotStateChange(DTOBotState botState) {
        logger.debug("Bot state changed: running={}, paused={}", 
            botState.getRunning(), botState.getPaused());
        
        messagingService.broadcast("botState", "botState.update", botState);
    }

    private DTOBotState buildCurrentBotState() {
        cl.camodev.wosbot.serv.task.TaskQueueManager queueManager =
                ServScheduler.getServices().getQueueManager();
        boolean hasRunningQueues = queueManager.hasRunningQueues();

        DTOBotState currentState = new DTOBotState();
        currentState.setRunning(hasRunningQueues);
        currentState.setPaused(false);
        currentState.setActionTime(LocalDateTime.now());
        return currentState;
    }
}
