package cl.camodev.wosbot.ot;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DTOTaskStatsAggregate {

    private Integer taskId;
    private String taskName;
    private long totalRuns;
    private long successCount;
    private long failureCount;
    private double successRate;
    private long minDurationMillis;
    private long maxDurationMillis;
    private double averageDurationMillis;
    private long p95DurationMillis;
    private LocalDateTime lastStartedAt;
    private LocalDateTime lastFinishedAt;
    private String lastErrorMessage;
    private long profileCount;
    private List<String> sampleProfiles = new ArrayList<>();

    public DTOTaskStatsAggregate() {
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

    public long getTotalRuns() {
        return totalRuns;
    }

    public void setTotalRuns(long totalRuns) {
        this.totalRuns = totalRuns;
    }

    public long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(long successCount) {
        this.successCount = successCount;
    }

    public long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(long failureCount) {
        this.failureCount = failureCount;
    }

    public double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(double successRate) {
        this.successRate = successRate;
    }

    public long getMinDurationMillis() {
        return minDurationMillis;
    }

    public void setMinDurationMillis(long minDurationMillis) {
        this.minDurationMillis = minDurationMillis;
    }

    public long getMaxDurationMillis() {
        return maxDurationMillis;
    }

    public void setMaxDurationMillis(long maxDurationMillis) {
        this.maxDurationMillis = maxDurationMillis;
    }

    public double getAverageDurationMillis() {
        return averageDurationMillis;
    }

    public void setAverageDurationMillis(double averageDurationMillis) {
        this.averageDurationMillis = averageDurationMillis;
    }

    public long getP95DurationMillis() {
        return p95DurationMillis;
    }

    public void setP95DurationMillis(long p95DurationMillis) {
        this.p95DurationMillis = p95DurationMillis;
    }

    public LocalDateTime getLastStartedAt() {
        return lastStartedAt;
    }

    public void setLastStartedAt(LocalDateTime lastStartedAt) {
        this.lastStartedAt = lastStartedAt;
    }

    public LocalDateTime getLastFinishedAt() {
        return lastFinishedAt;
    }

    public void setLastFinishedAt(LocalDateTime lastFinishedAt) {
        this.lastFinishedAt = lastFinishedAt;
    }

    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    public void setLastErrorMessage(String lastErrorMessage) {
        this.lastErrorMessage = lastErrorMessage;
    }

    public long getProfileCount() {
        return profileCount;
    }

    public void setProfileCount(long profileCount) {
        this.profileCount = profileCount;
    }

    public List<String> getSampleProfiles() {
        return sampleProfiles;
    }

    public void setSampleProfiles(List<String> sampleProfiles) {
        this.sampleProfiles = sampleProfiles != null ? new ArrayList<>(sampleProfiles) : new ArrayList<>();
    }
}
