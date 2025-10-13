package cl.camodev.wosbot.web.streaming;

import cl.camodev.wosbot.console.list.ILogListener;
import cl.camodev.wosbot.ot.DTOLogMessage;
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
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Service responsible for streaming logs to web clients via Server-Sent Events (SSE).
 */
@Service
@RestController
@RequestMapping("/logs")
public class LogStreamingService implements ILogListener {

    private static final Logger logger = LoggerFactory.getLogger(LogStreamingService.class);
    private static final int MAX_LOG_HISTORY = 1000;
    private static final long HEARTBEAT_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(25);

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Queue<DTOLogMessage> logHistory = new ConcurrentLinkedQueue<>();
    private final Gson gson;
    private final ScheduledExecutorService heartbeatExecutor;

    public LogStreamingService() {
        this.gson = JsonSerializerConfig.getGson();
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "log-stream-heartbeat");
            thread.setDaemon(true);
            return thread;
        });
    }

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamLogs() {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        emitters.add(emitter);
        
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> handleEmitterTimeout(emitter));
        emitter.onError(error -> handleEmitterError(emitter, error));
        
        // Send existing log history to new client
        for (DTOLogMessage log : logHistory) {
            sendLogToClient(emitter, log);
        }
        
        logger.info("New client connected to log stream. Total clients: {}", emitters.size());
        return emitter;
    }

    @PostConstruct
    public void initialize() {
        heartbeatExecutor.scheduleAtFixedRate(this::sendHeartbeat, HEARTBEAT_INTERVAL_MILLIS,
                HEARTBEAT_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
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
        for (SseEmitter emitter : emitters) {
            sendLogToClient(emitter, message);
        }
    }

    private void sendLogToClient(SseEmitter emitter, DTOLogMessage log) {
        try {
            String json = gson.toJson(log);
            emitter.send(SseEmitter.event().name("log").data(json));
        } catch (Exception e) {
            if (isClientAbortException(e)) {
                logger.trace("Client disconnected from log stream: {}", e.getMessage());
            } else {
                logger.debug("Error sending log to client: {}", e.getMessage());
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
                    logger.trace("Client disconnected during log heartbeat: {}", e.getMessage());
                } else {
                    logger.debug("Heartbeat send failure on log stream: {}", e.getMessage());
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
        logger.debug("Log stream emitter timed out");
        completeEmitter(emitter);
    }

    private void handleEmitterError(SseEmitter emitter, Throwable cause) {
        if (cause != null) {
            logger.debug("Log stream emitter error: {}", cause.getMessage());
        }
        completeEmitter(emitter);
    }

    private void completeEmitter(SseEmitter emitter) {
        emitters.remove(emitter);
        try {
            emitter.complete();
        } catch (IllegalStateException ignored) {
            // emitter already completed
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
        logHistory.clear();
        logger.info("Log streaming service shut down");
    }
}
