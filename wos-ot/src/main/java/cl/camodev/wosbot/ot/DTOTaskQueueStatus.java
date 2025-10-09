package cl.camodev.wosbot.ot;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;


public class DTOTaskQueueStatus {
    private boolean running;
    private boolean paused;
    private boolean needsReconnect;
    private boolean readyToReconnect;
    private boolean idleTimeExceeded;
    private Integer idleTimeLimit;
    private Integer backgroundChecks = 0;
    private Integer BACKGROUND_CHECKS_INTERVAL = 60; // every 60 loops

    private LocalDateTime pausedAt;
    private LocalDateTime delayUntil;
    private LocalDateTime reconnectAt;
    
    private final DelayQueue<Delayed> reconnectTimer = new DelayQueue<>();

    private LoopState loopState;

    public DTOTaskQueueStatus() {
        this.running = false;
        this.paused = false;
        this.needsReconnect = false;
        this.readyToReconnect = false;
        this.pausedAt = LocalDateTime.MIN;
        this.delayUntil = LocalDateTime.now();
        this.idleTimeLimit = 15; // default 15 minutes
    }

    public LoopState getLoopState() {
        return this.loopState;
    }

    public boolean isIdleTimeExceeded() {
        return this.idleTimeExceeded;
    }

    public void setIdleTimeExceeded(boolean idleTimeExceeded) {
        this.idleTimeExceeded = idleTimeExceeded;
    }

    /**
     * Sets the idle time limit in minutes. When the delay exceeds this limit,
     * the idleTimeExceeded flag will be set.
     *
     * @param idleTimeLimit The maximum idle time in minutes
     */
    public void setIdleTimeLimit(Integer idleTimeLimit) {
        this.idleTimeLimit = idleTimeLimit;
    }


    /**
     * Determines if background checks should be run based on a counter.
     * Uses a default interval of 60 loops between checks.
     * Increments internal counter and resets it when the interval is reached.
     *
     * @return true if background checks should run, false otherwise
     */
    public boolean shouldRunBackgroundChecks() {
        this.backgroundChecks++;
        if (this.backgroundChecks % this.BACKGROUND_CHECKS_INTERVAL == 0) {
            this.backgroundChecks = 1;
            return true;
        }
        return false;
    }

    @SuppressWarnings(value = { "unused" })
    public void setBACKGROUND_CHECKS_INTERVAL(Integer BACKGROUND_CHECKS_INTERVAL) {
        this.BACKGROUND_CHECKS_INTERVAL = BACKGROUND_CHECKS_INTERVAL;
    }

    public void setReconnectAt(long reconnectionTime) {
        this.setDelayUntil(reconnectionTime * 60);
        this.setReconnectAt(LocalDateTime.now().plusMinutes(reconnectionTime));
    }

    public void setReconnectAt(LocalDateTime reconnectAt) {
        this.pause();
        this.setNeedsReconnect(true);
        this.reconnectAt = reconnectAt;
        this.reconnectTimer.offer(new DelayedReconnect(reconnectAt));
        Thread.startVirtualThread(() -> {
            try {
                this.reconnectTimer.take();
                this.readyToReconnect = true;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @SuppressWarnings(value = { "unused" })
    public LocalDateTime getReconnectAt() {
        return this.reconnectAt;
    }

    public void loopStarted () {
        this.loopState = new LoopState();
    }

    public void reset() {
        this.paused = false;
        this.needsReconnect = false;
        this.pausedAt = null;
        this.delayUntil = null;
        this.running = false;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public void pause() {
        this.setPaused(true);
    }
    public void setPaused(boolean paused) {
        this.paused = paused;
        this.pausedAt = paused ? LocalDateTime.now() : null;
    }

    public boolean needsReconnect() {
        return this.needsReconnect;
    }

    public void setNeedsReconnect(boolean needsReconnect) {
        this.needsReconnect = needsReconnect;
    }

    public LocalDateTime getPausedAt() {
        return this.pausedAt;
    }

    public LocalDateTime getDelayUntil() {
        return this.delayUntil;
    }

    /**
     * Sets delay to a specific time from now
     *
     * @param delayUntil delay in seconds
     */
    public void setDelayUntil(long delayUntil) {
        this.delayUntil = LocalDateTime.now().plusSeconds(delayUntil);
    }

    public boolean checkIdleTimeExceeded() {
        return LocalDateTime.now().plusMinutes(this.idleTimeLimit).isBefore(this.getDelayUntil());
    }

    public void setDelayUntil(LocalDateTime delayUntil) {
        this.delayUntil = delayUntil;
    }

    public boolean isRunning() {
        return this.running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public boolean isReadyToReconnect() {
        return this.readyToReconnect;
    }

    record DelayedReconnect(LocalDateTime reconnectAt) implements Delayed {
        @Override
        public long getDelay(TimeUnit unit) {
            long delay = Duration.between(LocalDateTime.now(), reconnectAt).toMillis();
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }
        @Override
        @SuppressWarnings("NullableProblems")
        public int compareTo(Delayed other) {
            if (other instanceof DelayedReconnect) {
                return this.reconnectAt.compareTo(((DelayedReconnect) other).reconnectAt);
            }
            return 0;
        }
    }

    public static class LoopState {
        private final long startTime;
        private long endTime;

        private boolean executedTask;

        public LoopState() {
            this.startTime = System.currentTimeMillis();
        }

        public void endLoop() {
            this.endTime = System.currentTimeMillis();
        }

        public long getDuration() {
            return this.endTime - this.startTime;
        }

        public boolean isExecutedTask() {
            return this.executedTask;
        }

        public void setExecutedTask(boolean executedTask) {
            this.executedTask = executedTask;
        }
    }
}
