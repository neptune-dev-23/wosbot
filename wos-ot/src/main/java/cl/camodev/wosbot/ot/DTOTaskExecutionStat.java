package cl.camodev.wosbot.ot;

import java.time.LocalDateTime;

/**
 * Data transfer object representing the execution statistics of a task.
 */
public class DTOTaskExecutionStat {

    private Long id;
    private Long profileId;
    private String profileName;
    private String emulatorNumber;
    private Integer taskId;
    private String taskName;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private long durationMillis;
    private boolean success;
    private String errorMessage;

    public DTOTaskExecutionStat() {
    }

    public DTOTaskExecutionStat(Long profileId,
                                String profileName,
                                String emulatorNumber,
                                Integer taskId,
                                String taskName,
                                LocalDateTime scheduledAt,
                                LocalDateTime startedAt,
                                LocalDateTime finishedAt,
                                long durationMillis,
                                boolean success,
                                String errorMessage) {
        this.profileId = profileId;
        this.profileName = profileName;
        this.emulatorNumber = emulatorNumber;
        this.taskId = taskId;
        this.taskName = taskName;
        this.scheduledAt = scheduledAt;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.durationMillis = durationMillis;
        this.success = success;
        this.errorMessage = errorMessage;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProfileId() {
        return profileId;
    }

    public void setProfileId(Long profileId) {
        this.profileId = profileId;
    }

    public String getProfileName() {
        return profileName;
    }

    public void setProfileName(String profileName) {
        this.profileName = profileName;
    }

    public String getEmulatorNumber() {
        return emulatorNumber;
    }

    public void setEmulatorNumber(String emulatorNumber) {
        this.emulatorNumber = emulatorNumber;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public long getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(long durationMillis) {
        this.durationMillis = durationMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
