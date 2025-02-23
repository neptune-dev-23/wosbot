package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
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
import cl.camodev.wosbot.serv.task.impl.AllianceChestTask;
import cl.camodev.wosbot.serv.task.impl.AllianceTechTask;
import cl.camodev.wosbot.serv.task.impl.CrystalLaboratoryTask;
import cl.camodev.wosbot.serv.task.impl.ExplorationTask;
import cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;
import cl.camodev.wosbot.serv.task.impl.NomadicMerchantTask;
import cl.camodev.wosbot.serv.task.impl.PetAllianceTreasuresTask;
import cl.camodev.wosbot.serv.task.impl.PetSkillsTask;
import cl.camodev.wosbot.serv.task.impl.PetSkillsTask.PetSkill;
import cl.camodev.wosbot.serv.task.impl.TrainingTroopsTask;
import cl.camodev.wosbot.serv.task.impl.TrainingTroopsTask.TroopType;
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

		if (profiles.stream().filter(DTOProfiles::getEnabled).findAny().isEmpty()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, "ServScheduler", "-", "No Enabled profiles");
			return;
		} else {

			profiles.stream().filter(DTOProfiles::getEnabled).forEach(profile -> {
				String queueName = profile.getName();
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, "ServScheduler", "-", "starting queue ");
				queueManager.createQueue(profile);
				TaskQueue queue = queueManager.getQueue(queueName);
				queue.addTask(new InitializeTask(profile, TpDailyTaskEnum.INITIALIZE));

				//@formatter:off
				// Mapa de tareas con listas para manejar múltiples instancias de tareas bajo la misma clave
				Map<EnumConfigurationKey, List<Supplier<DelayedTask>>> taskMappings = new HashMap<>();

				// Agregar tareas al mapa
				taskMappings.put(EnumConfigurationKey.BOOL_EXPLORATION_CHEST, List.of(
				    () -> new ExplorationTask(profile, TpDailyTaskEnum.EXPLORATION_CHEST)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_HERO_RECRUITMENT, List.of(
				    () -> new HeroRecruitmentTask(profile, TpDailyTaskEnum.HERO_RECRUITMENT)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_WAR_ACADEMY_SHARDS, List.of(
				    () -> new WarAcademyTask(profile, TpDailyTaskEnum.WAR_ACADEMY_SHARDS)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_CRYSTAL_LAB_FC, List.of(
				    () -> new CrystalLaboratoryTask(profile, TpDailyTaskEnum.CRYSTAL_LABORATORY)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_NOMADIC_MERCHANT, List.of(
				    () -> new NomadicMerchantTask(profile, TpDailyTaskEnum.NOMADIC_MERCHANT)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_VIP_POINTS, List.of(() -> new VipTask(profile, TpDailyTaskEnum.VIP_POINTS)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_ALLIANCE_TECH, List.of(
				    () -> new AllianceTechTask(profile, TpDailyTaskEnum.ALLIANCE_TECH)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_ALLIANCE_CHESTS, List.of(
				    () -> new AllianceChestTask(profile, TpDailyTaskEnum.ALLIANCE_CHESTS)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_TRAINING_TROOPS, List.of(
				    () -> new TrainingTroopsTask(profile, TpDailyTaskEnum.TRAINING_TROOPS, TroopType.INFANTRY),
				    () -> new TrainingTroopsTask(profile, TpDailyTaskEnum.TRAINING_TROOPS, TroopType.LANCER),
				    () -> new TrainingTroopsTask(profile, TpDailyTaskEnum.TRAINING_TROOPS, TroopType.MARKSMAN)
				));

				taskMappings.put(EnumConfigurationKey.BOOL_ALLIANCE_PET_TREASURE, List.of(
				    () -> new PetAllianceTreasuresTask(profile, TpDailyTaskEnum.ALLIANCE_PET_TREASURE)
				));
				
				taskMappings.put(EnumConfigurationKey.PET_SKILL_FOOD_BOOL, List.of(
					() -> new PetSkillsTask(profile, TpDailyTaskEnum.PET_SKILL_FOOD, PetSkill.FOOD)
				));
				
				taskMappings.put(EnumConfigurationKey.PET_SKILL_GATHERING_BOOL, List.of(
					() -> new PetSkillsTask(profile, TpDailyTaskEnum.PET_SKILL_GATHERING, PetSkill.GATHERING)
				));
				
				taskMappings.put(EnumConfigurationKey.PET_SKILL_STAMINA_BOOL, List.of(
					() -> new PetSkillsTask(profile, TpDailyTaskEnum.PET_SKILL_STAMINA, PetSkill.STAMINA)
				));
				
				taskMappings.put(EnumConfigurationKey.PET_SKILL_TRESURE_BOOL, List.of(
					() -> new PetSkillsTask(profile, TpDailyTaskEnum.PET_SKILL_TREASURE, PetSkill.TREASURE)
				));
				
				
				
				//@formatter:on

				// Obtener el estado de las tareas desde la base de datos
				Map<Integer, DTODailyTaskStatus> taskSchedules = iDailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());

				// Recorrer el mapa y programar las tareas según la configuración del perfil
				taskMappings.forEach((key, taskSuppliers) -> {
					if (profile.getConfig(key, Boolean.class)) {
						for (Supplier<DelayedTask> taskSupplier : taskSuppliers) {
							DelayedTask task = taskSupplier.get();

							ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Scheduling tasks");

							if (taskSchedules.containsKey(task.getTpDailyTaskId())) {
								LocalDateTime nextSchedule = taskSchedules.get(task.getTpDailyTaskId()).getNextSchedule();
								task.reschedule(nextSchedule);
								ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Task is completed, rescheduling for tomorrow");
							} else {
								ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Task not completed, scheduling for today");
								task.reschedule(LocalDateTime.now());
							}

							queue.addTask(task);
						}
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
		dailyTask.setNextSchedule(nextSchedule);

		// Guardar la entidad (ya sea nueva o existente)
		iDailyTaskRepository.saveDailyTask(dailyTask);
	}

}
