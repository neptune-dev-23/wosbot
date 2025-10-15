package cl.camodev.wosbot.serv;

import cl.camodev.wosbot.almac.entity.SubTaskExecutionStat;

import java.util.List;

public interface SubTaskExecutionStatService {

    void save(SubTaskExecutionStat stat);

    List<SubTaskExecutionStat> getStats(Long profileId, String taskId, String subTaskType, Integer limit);
}
