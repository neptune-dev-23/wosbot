package cl.camodev.wosbot.ot;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Data transfer object representing the execution statistics of a task.
 */
public class DTOTaskExecutionStat {
    private DTOProfiles profile;
    private TpDailyTaskEnum task;
    private DTOTaskQueueStatus.LoopState loopState;
    private long scheduledAtMillis;
    private boolean success;

    public DTOTaskExecutionStat(
            DTOProfiles profile,
            TpDailyTaskEnum task,
            long scheduledAtMillis,
            DTOTaskQueueStatus.LoopState loopState) {
        this.profile = profile;
        this.task = task;
        this.loopState = loopState;
        this.scheduledAtMillis = scheduledAtMillis;
    }

    public Long getProfileId() {
        return this.profile.getId();
    }

    public String getProfileName() {
        return this.profile.getName();
    }

    public String getEmulatorNumber() {
        return this.profile.getEmulatorNumber();
    }

    public Integer getTaskId() {
        return this.task.getId();
    }

    public String getTaskName() {
        return this.task.getName();
    }

    public LocalDateTime getScheduledAt() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.scheduledAtMillis), ZoneId.systemDefault());
    }

    public LocalDateTime getStartedAt() {
        return this.loopState.getTaskStartedAt();
    }

    public LocalDateTime getFinishedAt() {
        return this.loopState.getEndTime();
    }

    public long getDurationMillis() {
        return this.loopState.getDurationMillis();
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return this.loopState.getErrorMessage();
    }
}
