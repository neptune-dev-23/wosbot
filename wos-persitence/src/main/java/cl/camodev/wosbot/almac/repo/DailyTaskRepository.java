package cl.camodev.wosbot.almac.repo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cl.camodev.wosbot.almac.entity.DailyTask;
import cl.camodev.wosbot.almac.entity.TpDailyTask;
import cl.camodev.wosbot.almac.jpa.BotPersistence;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;

public class DailyTaskRepository implements IDailyTaskRepository {
	private final BotPersistence persistence = BotPersistence.getInstance();

	private static DailyTaskRepository instance;

	private DailyTaskRepository() {
	}

	public static DailyTaskRepository getRepository() {
		if (instance == null) {
			instance = new DailyTaskRepository();
		}
		return instance;
	}

	@Override
	public boolean addDailyTask(DailyTask dailyTask) {
		return persistence.createEntity(dailyTask);
	}

	@Override
	public boolean saveDailyTask(DailyTask dailyTask) {
		return persistence.updateEntity(dailyTask);
	}

	@Override
	public boolean deleteDailyTask(DailyTask dailyTask) {
		return persistence.deleteEntity(dailyTask);
	}

	@Override
	public DailyTask getDailyTaskById(Long id) {
		return persistence.findEntityById(DailyTask.class, id);
	}

	@Override
	public List<DailyTask> findByProfileId(Long profileId) {
		String query = "SELECT d FROM DailyTask d WHERE d.profile.id = :profileId";

		// Crear el mapa de parámetros
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("profileId", profileId);

		return persistence.getQueryResults(query, DailyTask.class, parameters);
	}

	@Override
	public DailyTask findByProfileIdAndTaskName(Long profileId, TpDailyTaskEnum taskName) {
		String query = "SELECT d FROM DailyTask d WHERE d.profile.id = :profileId AND d.task.id = :id";

		// Crear el mapa de parámetros
		Map<String, Object> parameters = new HashMap<>();
		parameters.put("profileId", profileId);
		parameters.put("id", taskName.getId());

		List<DailyTask> results = persistence.getQueryResults(query, DailyTask.class, parameters);
		return results.isEmpty() ? null : results.get(0);
	}

	@Override
	public TpDailyTask findTpDailyTaskById(Integer id) {
		return persistence.findEntityById(TpDailyTask.class, id);
	}
}
