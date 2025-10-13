package cl.camodev.wosbot.web.streaming;

import cl.camodev.wosbot.ot.DTOBotState;
import cl.camodev.wosbot.serv.IBotStateListener;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.web.config.JsonSerializerConfig;
import com.google.gson.Gson;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Locale;

/**
 * Service responsible for streaming bot state changes via Server-Sent Events (SSE).
 */
@Service
@RestController
@RequestMapping("/api/bot/state")
public class BotStateStreamingService implements IBotStateListener {

    private static final Logger logger = LoggerFactory.getLogger(BotStateStreamingService.class);
    private static final long HEARTBEAT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(25);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Gson gson;
    private final ScheduledExecutorService heartbeatExecutor;

    public BotStateStreamingService() {
        this.gson = JsonSerializerConfig.getGson();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "bot-state-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamBotState() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> handleEmitterTimeout(emitter));
        emitter.onError(error -> handleEmitterError(emitter, error));
        
        // Send current bot state to new client
        sendCurrentBotState(emitter);
        
        logger.info("New client connected to bot state stream. Total clients: {}", emitters.size());
        return emitter;
    }

    @PostConstruct
    public void initialize() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL_MILLIS,
                HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void sendCurrentBotState(SseEmitter emitter) {
        try {
            cl.camodev.wosbot.serv.task.TaskQueueManager queueManager = 
                ServScheduler.getServices().getQueueManager();
            boolean hasRunningQueues = queueManager.hasRunningQueues();
            
            DTOBotState currentState = new DTOBotState();
            currentState.setRunning(hasRunningQueues);
            currentState.setPaused(false);
            currentState.setActionTime(LocalDateTime.now());
            
            sendBotStateToClient(emitter, currentState);
        } catch (Exception e) {
            logger.error("Error sending current bot state: {}", e.getMessage());
        }
    }

    @Override
    public void onBotStateChange(DTOBotState botState) {
        logger.debug("Bot state changed: running={}, paused={}", 
            botState.getRunning(), botState.getPaused());
        
        // Broadcast to all connected clients
        for (SseEmitter emitter : emitters) {
            sendBotStateToClient(emitter, botState);
        }
    }

    private void sendBotStateToClient(SseEmitter emitter, DTOBotState botState) {
        try {
            String json = gson.toJson(botState);
            emitter.send(SseEmitter.event().name("botState").data(json));
        } catch (Exception e) {
            if (isClientAbortException(e)) {
                logger.trace("Client disconnected from bot state stream: {}", e.getMessage());
            } else {
                logger.debug("Error sending bot state to client: {}", e.getMessage());
            }
            handleEmitterError(emitter, e);
        }
    }

    private void sendHeartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("heartbeat"));
            } catch (Exception e) {
                if (isClientAbortException(e)) {
                    logger.trace("Client disconnected during bot state heartbeat: {}", e.getMessage());
                } else {
                    logger.debug("Heartbeat send failure on bot state stream: {}", e.getMessage());
                }
                handleEmitterError(emitter, e);
            }
        }
    }

    private boolean isClientAbortException(Throwable throwable) {
        if (throwable == null) {
            return false;
        }
        if (throwable instanceof org.apache.catalina.connector.ClientAbortException) {
            return true;
        }
        if (throwable instanceof IOException ioException) {
            String message = ioException.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("broken pipe")
                        || lower.contains("connection reset")
                        || lower.contains("connection aborted")) {
                    return true;
                }
            }
        }
        Throwable cause = throwable.getCause();
        return cause != null && isClientAbortException(cause);
    }

    private void handleEmitterTimeout(SseEmitter emitter) {
        logger.debug("Bot state emitter timed out");
        completeEmitter(emitter);
    }

    private void handleEmitterError(SseEmitter emitter, Throwable cause) {
        if (cause != null) {
            logger.debug("Bot state emitter error: {}", cause.getMessage());
        }
        completeEmitter(emitter);
    }

    private void completeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // already completed
        } catch (Exception ex) {
            logger.trace("Ignoring emitter completion error: {}", ex.getMessage());
        }
    }

    @PreDestroy
    public void shutdown() {
        for (SseEmitter emitter : emitters) {
            completeEmitter(emitter);
        }
        heartbeatExecutor.shutdownNow();
        logger.info("Bot state streaming service shut down");
    }
}
