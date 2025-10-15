package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cl.camodev.wosbot.almac.entity.TaskExecutionStat;
import cl.camodev.wosbot.almac.repo.ITaskStatsRepository;
import cl.camodev.wosbot.almac.repo.TaskStatsRepository;
import cl.camodev.wosbot.ot.DTOTaskExecutionStat;
import cl.camodev.wosbot.ot.DTOTaskStatsAggregate;

public class ServTaskStats {

    private static final Logger logger = LoggerFactory.getLogger(ServTaskStats.class);
    private static final int DEFAULT_LIMIT = 1000;
    private static final long MINIMUM_DURATION_MILLIS = 5_000L;

    private static ServTaskStats instance;
    private final ITaskStatsRepository repository;

    private ServTaskStats() {
        this.repository = TaskStatsRepository.getRepository();
    }

    public static ServTaskStats getInstance() {
        if (instance == null) {
            instance = new ServTaskStats();
        }
        return instance;
    }

    public void recordExecution(DTOTaskExecutionStat stat) {
        if (stat == null) {
            return;
        }

        boolean stored = repository.recordExecution(stat);
        if (!stored) {
            logger.warn("Failed to persist task execution statistics for task {}", stat.getTaskName());
        }
    }

    public List<DTOTaskStatsAggregate> getTaskSummaries(Long profileId, Integer taskId, int limit) {
        int effectiveLimit = limit > 0 ? limit : DEFAULT_LIMIT;
        List<TaskExecutionStat> executions = repository.findExecutions(profileId, taskId, effectiveLimit, MINIMUM_DURATION_MILLIS);

        if (executions == null || executions.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, StatsAccumulator> accumulatorMap = new LinkedHashMap<>();

        for (TaskExecutionStat execution : executions) {
            if (execution.getDurationMillis() < MINIMUM_DURATION_MILLIS) {
                continue;
            }
            String key = buildKey(execution.getTaskId(), execution.getTaskName());
            accumulatorMap.computeIfAbsent(key,
                    k -> new StatsAccumulator(execution.getTaskId(), execution.getTaskName()))
                    .add(execution);
        }

        return accumulatorMap.values().stream()
                .map(StatsAccumulator::toDto)
                .filter(dto -> dto.getTotalRuns() > 0)
                .toList();
    }

    private String buildKey(Integer taskId, String taskName) {
        String taskKey = taskId != null ? String.valueOf(taskId) : Objects.toString(taskName, "unknown");
        return taskKey;
    }

    private static final class StatsAccumulator {
        private final Integer taskId;
        private final String taskName;
        private long totalRuns;
        private long successCount;
        private long failureCount;
        private long minDuration = Long.MAX_VALUE;
        private long maxDuration = Long.MIN_VALUE;
        private double sumDuration;
        private final List<Long> durations = new ArrayList<>();
        private LocalDateTime lastStartedAt;
        private LocalDateTime lastFinishedAt;
        private String lastErrorMessage;
        private final List<Long> profileIds = new ArrayList<>();
        private final List<String> profileNames = new ArrayList<>();

        private StatsAccumulator(Integer taskId,
                                 String taskName) {
            this.taskId = taskId;
            this.taskName = taskName;
        }

        private void add(TaskExecutionStat execution) {
            totalRuns++;
            long duration = execution.getDurationMillis();
            sumDuration += duration;
            durations.add(duration);
            minDuration = Math.min(minDuration, duration);
            maxDuration = Math.max(maxDuration, duration);

            if (execution.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                if (lastErrorMessage == null
                        || isAfter(execution.getStartedAt(), lastStartedAt)) {
                    lastErrorMessage = execution.getErrorMessage();
                }
            }

            lastStartedAt = max(lastStartedAt, execution.getStartedAt());
            lastFinishedAt = max(lastFinishedAt, execution.getFinishedAt());
            if (execution.getProfileId() != null) {
                profileIds.add(execution.getProfileId());
            }
            if (execution.getProfileName() != null) {
                profileNames.add(execution.getProfileName());
            }
        }

        private LocalDateTime max(LocalDateTime current, LocalDateTime candidate) {
            if (candidate == null) {
                return current;
            }
            if (current == null) {
                return candidate;
            }
            return candidate.isAfter(current) ? candidate : current;
        }

        private boolean isAfter(LocalDateTime candidate, LocalDateTime reference) {
            if (candidate == null) {
                return false;
            }
            if (reference == null) {
                return true;
            }
            return candidate.isAfter(reference);
        }

        private DTOTaskStatsAggregate toDto() {
            DTOTaskStatsAggregate aggregate = new DTOTaskStatsAggregate();
            aggregate.setTaskId(taskId);
            aggregate.setTaskName(taskName);
            aggregate.setTotalRuns(totalRuns);
            aggregate.setSuccessCount(successCount);
            aggregate.setFailureCount(failureCount);
            aggregate.setSuccessRate(totalRuns == 0 ? 0.0 : (double) successCount / totalRuns);
            aggregate.setMinDurationMillis(totalRuns == 0 ? 0 : minDuration);
            aggregate.setMaxDurationMillis(totalRuns == 0 ? 0 : maxDuration);
            aggregate.setAverageDurationMillis(totalRuns == 0 ? 0 : sumDuration / totalRuns);
            aggregate.setP95DurationMillis(calculateP95());
            aggregate.setLastStartedAt(lastStartedAt);
            aggregate.setLastFinishedAt(lastFinishedAt);
            aggregate.setLastErrorMessage(lastErrorMessage);
            long distinctProfiles = profileIds.stream().filter(Objects::nonNull).distinct().count();
            if (distinctProfiles == 0) {
                distinctProfiles = profileNames.stream()
                        .filter(Objects::nonNull)
                        .map(String::trim)
                        .filter(name -> !name.isEmpty())
                        .distinct()
                        .count();
            }
            aggregate.setProfileCount(distinctProfiles);
            aggregate.setSampleProfiles(buildProfileSamples());
            return aggregate;
        }

        private List<String> buildProfileSamples() {
            if (profileNames.isEmpty()) {
                return Collections.emptyList();
            }
            return profileNames.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(name -> !name.isEmpty())
                    .distinct()
                    .limit(5)
                    .toList();
        }

        private long calculateP95() {
            if (durations.isEmpty()) {
                return 0;
            }
            List<Long> sorted = new ArrayList<>(durations);
            sorted.sort(Comparator.naturalOrder());
            int index = (int) Math.ceil(sorted.size() * 0.95) - 1;
            index = Math.max(index, 0);
            return sorted.get(index);
        }
    }
}
