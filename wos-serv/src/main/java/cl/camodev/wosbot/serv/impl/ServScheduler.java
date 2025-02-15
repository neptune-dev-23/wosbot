package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cl.camodev.wosbot.almac.entity.DailyTask;
import cl.camodev.wosbot.almac.entity.Profile;
import cl.camodev.wosbot.almac.entity.TpDailyTask;
import cl.camodev.wosbot.almac.repo.DailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IDailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IProfileRepository;
import cl.camodev.wosbot.almac.repo.ProfileRepository;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOBotState;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.IBotStateListener;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.TaskQueue;
import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.wosbot.serv.task.impl.AllianceTechTask;
import cl.camodev.wosbot.serv.task.impl.CrystalLaboratoryTask;
import cl.camodev.wosbot.serv.task.impl.ExplorationTask;
import cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;
import cl.camodev.wosbot.serv.task.impl.NomadicMerchantTask;
import cl.camodev.wosbot.serv.task.impl.VipTask;
import cl.camodev.wosbot.serv.task.impl.WarAcademyTask;

public class ServScheduler {
	private static ServScheduler instance;

	private final TaskQueueManager queueManager = new TaskQueueManager();

	private List<IBotStateListener> listeners = new ArrayList<IBotStateListener>();

	private IDailyTaskRepository iDailyTaskRepository = DailyTaskRepository.getRepository();

	private IProfileRepository iProfileRepository = ProfileRepository.getRepository();

	private ServScheduler() {

	}

	public static ServScheduler getServices() {
		if (instance == null) {
			instance = new ServScheduler();
		}
		return instance;
	}

	public void startBot() {
		List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();

		if (profiles == null || profiles.isEmpty()) {
			return;
		}

		profiles.stream().filter(DTOProfiles::getEnabled).forEach(profile -> {
			String queueName = profile.getName();
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, "ServScheduler", "-", "starting queue ");

			queueManager.createQueue(profile);
			TaskQueue queue = queueManager.getQueue(queueName);

			// Inicializar tarea
			InitializeTask initTask = new InitializeTask(profile, LocalDateTime.now());
			queue.addTask(initTask);
			queue.setInitializeTask(initTask);

			//@formatter:off
					Map<EnumConfigurationKey, Supplier<DelayedTask>> taskMappings = Map.of(
							EnumConfigurationKey.BOOL_EXPLORATION_CHEST, () -> new ExplorationTask(profile, LocalDateTime.now()), 
							EnumConfigurationKey.BOOL_HERO_RECRUITMENT, () -> new HeroRecruitmentTask(profile, LocalDateTime.now()),
							EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS, () -> new WarAcademyTask(profile, LocalDateTime.now()), 
							EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC, () -> new CrystalLaboratoryTask(profile, LocalDateTime.now()), 
							EnumConfigurationKey.BOOL_NOMADIC_MERCHANT,	() -> new NomadicMerchantTask(profile, LocalDateTime.now()));

				// @formatter:on
			taskMappings.forEach((key, taskSupplier) -> {
				if (profile.getConfig(key, Boolean.class)) {
					ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, taskSupplier.get().getTaskName(), queueName, "creating task's");
					queue.addTask(taskSupplier.get());
				}
			});

			queue.addTask(new VipTask(profile, LocalDateTime.now()));
			queue.addTask(new AllianceTechTask(profile, LocalDateTime.now()));
			queueManager.startQueue(queueName);
		});
		listeners.forEach(e -> {
			DTOBotState state = new DTOBotState();
			state.setRunning(true);
			state.setActionTime(LocalDateTime.now());
			e.onBotStateChange(state);
		});
	}

	public void registryBotStateListener(IBotStateListener listener) {

		if (listeners == null) {
			listeners = new ArrayList<IBotStateListener>();
		}
		listeners.add(listener);
	}

	public void stopBot() {
		queueManager.stopQueues();

		List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();

		if (profiles == null || profiles.isEmpty()) {
			return;
		}

		profiles.stream().forEach(profile -> {
			ServProfiles.getServices().notifyProfileStatusChange(new DTOProfileStatus(profile.getId(), "NOT RUNNING "));
		});

		listeners.forEach(e -> {
			DTOBotState state = new DTOBotState();
			state.setRunning(false);
			state.setActionTime(LocalDateTime.now());
			e.onBotStateChange(state);
		});
	}

	public DTODailyTaskStatus getDailyTaskStatus(DTOProfiles profile, TpDailyTaskEnum task) {
		DailyTask dailyDask = iDailyTaskRepository.findByProfileIdAndTaskName(profile.getId(), task);

		if (dailyDask != null) {
			return new DTODailyTaskStatus(dailyDask.getLastExecution(), dailyDask.getFinished());
		} else {
			return new DTODailyTaskStatus(LocalDateTime.now(), false);
		}

	}

	public void updateDailyTaskStatus(DTOProfiles profile, TpDailyTaskEnum task, boolean finished) {
		DailyTask dailyTask = iDailyTaskRepository.findByProfileIdAndTaskName(profile.getId(), task);

		if (dailyTask == null) {
			// Crear nueva tarea si no existe
			dailyTask = new DailyTask();

			Profile profileEntity = iProfileRepository.getProfileById(profile.getId());
			TpDailyTask tpDailyTaskEntity = iDailyTaskRepository.findTpDailyTaskById(task.getId());
			dailyTask.setProfile(profileEntity);
			dailyTask.setTask(tpDailyTaskEntity);
			iDailyTaskRepository.addDailyTask(dailyTask);
		}

		// Actualizar los valores en cualquier caso
		dailyTask.setFinished(finished);
		dailyTask.setLastExecution(LocalDateTime.now());

		// Guardar la entidad (ya sea nueva o existente)
		iDailyTaskRepository.saveDailyTask(dailyTask);
	}

}
