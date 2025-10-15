package cl.camodev.wosbot.serv.impl;

import cl.camodev.wosbot.almac.entity.SubTaskExecutionStat;
import cl.camodev.wosbot.almac.jpa.StatsPersistence;
import cl.camodev.wosbot.serv.SubTaskExecutionStatService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SubTaskExecutionStatServiceImpl implements SubTaskExecutionStatService {

    private final StatsPersistence statsPersistence = StatsPersistence.getInstance();

    @Override
    public void save(SubTaskExecutionStat stat) {
        statsPersistence.createEntity(stat);
    }

    @Override
    public List<SubTaskExecutionStat> getStats(Long profileId, String taskId, String subTaskType, Integer limit) {
        StringBuilder queryString = new StringBuilder("SELECT s FROM SubTaskExecutionStat s WHERE 1=1");
        Map<String, Object> parameters = new HashMap<>();

        if (profileId != null) {
            queryString.append(" AND s.profileId = :profileId");
            parameters.put("profileId", profileId);
        }

        if (taskId != null && !taskId.isEmpty()) {
            queryString.append(" AND s.taskId = :taskId");
            parameters.put("taskId", taskId);
        }

        if (subTaskType != null && !subTaskType.isEmpty()) {
            queryString.append(" AND s.subTaskType = :subTaskType");
            parameters.put("subTaskType", subTaskType);
        }

        queryString.append(" ORDER BY s.createdAt DESC");

        return statsPersistence.getQueryResults(queryString.toString(), SubTaskExecutionStat.class, parameters, limit);
    }
}
