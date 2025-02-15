package cl.camodev.wosbot.almac.repo;

import java.util.List;

import cl.camodev.wosbot.almac.entity.DailyTask;
import cl.camodev.wosbot.almac.entity.TpDailyTask;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;

public interface IDailyTaskRepository {

	boolean addDailyTask(DailyTask dailyTask);

	boolean saveDailyTask(DailyTask dailyTask);

	boolean deleteDailyTask(DailyTask dailyTask);

	DailyTask getDailyTaskById(Long id);

	List<DailyTask> findByProfileId(Long profileId);

	DailyTask findByProfileIdAndTaskName(Long profileId, TpDailyTaskEnum taskName);

	TpDailyTask findTpDailyTaskById(Integer id);
}
