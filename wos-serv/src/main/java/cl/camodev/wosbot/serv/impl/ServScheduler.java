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
			queue.addTask(new InitializeTask(profile, TpDailyTaskEnum.INITIALIZE));

			//@formatter:off
			Map<EnumConfigurationKey, Supplier<DelayedTask>> taskMappings = Map.of(
					EnumConfigurationKey.BOOL_EXPLORATION_CHEST, 	() -> new ExplorationTask(profile	       ,TpDailyTaskEnum.EXPLORATION_CHEST), 
					EnumConfigurationKey.BOOL_HERO_RECRUITMENT, 	() -> new HeroRecruitmentTask(profile      ,TpDailyTaskEnum.HERO_RECRUITMENT),
					EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS, 	() -> new WarAcademyTask(profile           ,TpDailyTaskEnum.WAR_ACADEMY_SHARDS), 
					EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC, 		() -> new CrystalLaboratoryTask(profile    ,TpDailyTaskEnum.CRYSTAL_LABORATORY), 
					EnumConfigurationKey.BOOL_NOMADIC_MERCHANT,		() -> new NomadicMerchantTask(profile      ,TpDailyTaskEnum.NOMADIC_MERCHANT),
					EnumConfigurationKey.BOOL_VIP_POINTS, 			() -> new VipTask(profile                  ,TpDailyTaskEnum.VIP_POINTS),
					EnumConfigurationKey.BOOL_ALLIANCE_TECH, 		() -> new AllianceTechTask(profile         ,TpDailyTaskEnum.ALLIANCE_TECH)
                    );

			// @formatter:on

			Map<Long, DTODailyTaskStatus> taksSchedules = iDailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());
			taskMappings.forEach((key, taskSupplier) -> {
				if (profile.getConfig(key, Boolean.class)) {
					ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskSupplier.get().getTaskName(), profile.getName(), "Schediling tasks");
					if (taksSchedules.containsKey(taskSupplier.get().getTpDailyTaskId())) {
						LocalDateTime nextSchedule = taksSchedules.get(taskSupplier.get().getTpDailyTaskId()).getNextSchedule();
						taskSupplier.get().reschedule(nextSchedule);
						ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskSupplier.get().getTaskName(), profile.getName(), "Task is completed, rescheduling for tomorrow");
					} else {
						ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskSupplier.get().getTaskName(), profile.getName(), "Task not completed, scheduling for today");
						taskSupplier.get().reschedule(LocalDateTime.now());
					}

					queue.addTask(taskSupplier.get());
				}
			});

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

	public void updateDailyTaskStatus(DTOProfiles profile, TpDailyTaskEnum task, LocalDateTime nextSchedule) {

		DailyTask dailyTask = iDailyTaskRepository.findByProfileIdAndTaskName(profile.getId(), task);

		if (dailyTask == null) {
			// Crear nueva tarea si no existe
			dailyTask = new DailyTask();

			Profile profileEntity = iProfileRepository.getProfileById(profile.getId());
			TpDailyTask tpDailyTaskEntity = iDailyTaskRepository.findTpDailyTaskById(task.getId());
			dailyTask.setProfile(profileEntity);
			dailyTask.setTask(tpDailyTaskEntity);
			dailyTask.setLastExecution(LocalDateTime.now());
			dailyTask.setNextSchedule(nextSchedule);
			iDailyTaskRepository.addDailyTask(dailyTask);
		}

		dailyTask.setLastExecution(LocalDateTime.now());

		// Guardar la entidad (ya sea nueva o existente)
		iDailyTaskRepository.saveDailyTask(dailyTask);
	}

}
