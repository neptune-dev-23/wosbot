package cl.camodev.wosbot.almac.repo;

import java.util.List;

import cl.camodev.wosbot.almac.entity.TaskExecutionStat;
import cl.camodev.wosbot.ot.DTOTaskExecutionStat;

public interface ITaskStatsRepository {

    boolean recordExecution(DTOTaskExecutionStat stat);

    List<TaskExecutionStat> findExecutions(Long profileId, Integer taskId, int limit, long minimumDurationMillis);
}
