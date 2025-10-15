package cl.camodev.wosbot.almac.repo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.camodev.wosbot.almac.entity.TaskExecutionStat;
import cl.camodev.wosbot.almac.jpa.StatsPersistence;
import cl.camodev.wosbot.ot.DTOTaskExecutionStat;

public class TaskStatsRepository implements ITaskStatsRepository {

    private static TaskStatsRepository instance;
    private final StatsPersistence persistence = StatsPersistence.getInstance();

    private TaskStatsRepository() {
    }

    public static TaskStatsRepository getRepository() {
        if (instance == null) {
            instance = new TaskStatsRepository();
        }
        return instance;
    }

    @Override
    public boolean recordExecution(DTOTaskExecutionStat stat) {
        if (stat == null) {
            return false;
        }

        TaskExecutionStat entity = new TaskExecutionStat(
                stat.getProfileId(),
                stat.getProfileName(),
                stat.getEmulatorNumber(),
                stat.getTaskId(),
                stat.getTaskName(),
                stat.getScheduledAt(),
                stat.getStartedAt(),
                stat.getFinishedAt(),
                stat.getDurationMillis(),
                stat.isSuccess(),
                stat.getErrorMessage());

        return persistence.createEntity(entity);
    }

    @Override
    public List<TaskExecutionStat> findExecutions(Long profileId, Integer taskId, int limit, long minimumDurationMillis) {
        StringBuilder queryBuilder = new StringBuilder("SELECT s FROM TaskExecutionStat s WHERE 1 = 1");
        Map<String, Object> parameters = new HashMap<>();

        if (profileId != null) {
            queryBuilder.append(" AND s.profileId = :profileId");
            parameters.put("profileId", profileId);
        }

        if (taskId != null) {
            queryBuilder.append(" AND s.taskId = :taskId");
            parameters.put("taskId", taskId);
        }

        queryBuilder.append(" AND s.durationMillis >= :minimumDuration");
        parameters.put("minimumDuration", minimumDurationMillis);

        queryBuilder.append(" ORDER BY s.startedAt DESC");

        Integer maxResults = (limit > 0) ? limit : null;

        return persistence.getQueryResults(queryBuilder.toString(), TaskExecutionStat.class, parameters, maxResults);
    }
}
