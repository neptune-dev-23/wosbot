package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cl.camodev.wosbot.almac.entity.Config;
import cl.camodev.wosbot.almac.entity.DailyTask;
import cl.camodev.wosbot.almac.entity.Profile;
import cl.camodev.wosbot.almac.entity.TpConfig;
import cl.camodev.wosbot.almac.entity.TpDailyTask;
import cl.camodev.wosbot.almac.repo.ConfigRepository;
import cl.camodev.wosbot.almac.repo.DailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IConfigRepository;
import cl.camodev.wosbot.almac.repo.IDailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IProfileRepository;
import cl.camodev.wosbot.almac.repo.ProfileRepository;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpConfigEnum;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOBotState;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.serv.IBotStateListener;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.TaskQueue;
import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.wosbot.serv.task.impl.AllianceAutojoinTask;
import cl.camodev.wosbot.serv.task.impl.AllianceChestTask;
import cl.camodev.wosbot.serv.task.impl.AllianceHelpTask;
import cl.camodev.wosbot.serv.task.impl.AllianceTechTask;
import cl.camodev.wosbot.serv.task.impl.CrystalLaboratoryTask;
import cl.camodev.wosbot.serv.task.impl.DailyMissionTask;
import cl.camodev.wosbot.serv.task.impl.DailyStaminaTask;
import cl.camodev.wosbot.serv.task.impl.ExplorationTask;
import cl.camodev.wosbot.serv.task.impl.GatherSpeedTask;
import cl.camodev.wosbot.serv.task.impl.GatherTask;
import cl.camodev.wosbot.serv.task.impl.GatherTask.GatherType;
import cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;
import cl.camodev.wosbot.serv.task.impl.IntelligenceTask;
import cl.camodev.wosbot.serv.task.impl.LifeEssenceCaringTask;
import cl.camodev.wosbot.serv.task.impl.LifeEssenceTask;
import cl.camodev.wosbot.serv.task.impl.MailRewardsTask;
import cl.camodev.wosbot.serv.task.impl.NomadicMerchantTask;
import cl.camodev.wosbot.serv.task.impl.OnlineRewardTask;
import cl.camodev.wosbot.serv.task.impl.PetAdventureChestTask;
import cl.camodev.wosbot.serv.task.impl.PetAllianceTreasuresTask;
import cl.camodev.wosbot.serv.task.impl.PetSkillsTask;
import cl.camodev.wosbot.serv.task.impl.PetSkillsTask.PetSkill;
import cl.camodev.wosbot.serv.task.impl.TrainingTroopsTask;
import cl.camodev.wosbot.serv.task.impl.TrainingTroopsTask.TroopType;
import cl.camodev.wosbot.serv.task.impl.TriumphTask;
import cl.camodev.wosbot.serv.task.impl.UpgradeFurnaceTask;
import cl.camodev.wosbot.serv.task.impl.VipTask;
import cl.camodev.wosbot.serv.task.impl.WarAcademyTask;

public class ServScheduler {
	private static ServScheduler instance;

	private final TaskQueueManager queueManager = new TaskQueueManager();

	private List<IBotStateListener> listeners = new ArrayList<IBotStateListener>();

	private IDailyTaskRepository iDailyTaskRepository = DailyTaskRepository.getRepository();

	private IProfileRepository iProfileRepository = ProfileRepository.getRepository();

	private IConfigRepository iConfigRepository = ConfigRepository.getRepository();

	private ServScheduler() {

	}

	public static ServScheduler getServices() {
		if (instance == null) {
			instance = new ServScheduler();
		}
		return instance;
	}

	public void startBot() {
		EmulatorManager emulator = EmulatorManager.getInstance();

		try {
			emulator.initialze();
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		HashMap<String, String> globalsettings = ServConfig.getServices().getGlobalConfig();
		globalsettings.forEach((key, value) -> {
			if (key.equals(EnumConfigurationKey.MUMU_PATH_STRING.name())) {
				saveEmulatorPath(EnumConfigurationKey.MUMU_PATH_STRING.name(), value);
			} else if (key.equals(EnumConfigurationKey.LDPLAYER_PATH_STRING.name())) {
				saveEmulatorPath(EnumConfigurationKey.LDPLAYER_PATH_STRING.name(), value);
			}
		});
		List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();

		if (profiles == null || profiles.isEmpty()) {
			return;
		}

		if (profiles.stream().filter(DTOProfiles::getEnabled).findAny().isEmpty()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, "ServScheduler", "-", "No Enabled profiles");
			return;
		} else {
			profiles.stream().filter(DTOProfiles::getEnabled).sorted(Comparator.comparing(DTOProfiles::getId)).forEach(profile -> {
				profile.setGlobalSettings(globalsettings);
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, "ServScheduler", "-", "starting queue ");
				queueManager.createQueue(profile);
				TaskQueue queue = queueManager.getQueue(profile.getId());
				queue.addTask(new InitializeTask(profile, TpDailyTaskEnum.INITIALIZE));

				//@formatter:off
				// Mapa de tareas con listas para manejar múltiples instancias de tareas bajo la misma clave
				Map<EnumConfigurationKey, List<Supplier<DelayedTask>>> taskMappings = new HashMap<>();
				
				taskMappings.put(EnumConfigurationKey.DAILY_MISSION_BOOL, List.of(
                    () -> new DailyMissionTask(profile, TpDailyTaskEnum.DAILY_MISSIONS)
                ));
				
				taskMappings.put(EnumConfigurationKey.GATHER_SPEED_BOOL, List.of(
					() -> new GatherSpeedTask(profile, TpDailyTaskEnum.GATHER_BOOST)
				));

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

				taskMappings.put(EnumConfigurationKey.ALLIANCE_TECH_BOOL, List.of(
				    () -> new AllianceTechTask(profile, TpDailyTaskEnum.ALLIANCE_TECH)
				));
				
				
				taskMappings.put(EnumConfigurationKey.ALLIANCE_LIFE_ESSENCE_BOOL, List.of(
                    () -> new LifeEssenceCaringTask(profile, TpDailyTaskEnum.LIFE_ESSENCE_CARING)
                ));

				taskMappings.put(EnumConfigurationKey.ALLIANCE_CHESTS_BOOL, List.of(
				    () -> new AllianceChestTask(profile, TpDailyTaskEnum.ALLIANCE_CHESTS)
				));
				
				taskMappings.put(EnumConfigurationKey.ALLIANCE_HELP_REQUESTS_BOOL, List.of(
                    () -> new AllianceHelpTask(profile, TpDailyTaskEnum.ALLIANCE_HELP)
                ));
				
				taskMappings.put(EnumConfigurationKey.ALLIANCE_TRIUMPH_BOOL, List.of(
                    () -> new TriumphTask(profile, TpDailyTaskEnum.ALLIANCE_TRIUMPH)
                ));

				taskMappings.put(EnumConfigurationKey.BOOL_TRAINING_TROOPS, List.of(
				    () -> new TrainingTroopsTask(profile, TpDailyTaskEnum.TRAINING_INFANTRY, TroopType.INFANTRY),
				    () -> new TrainingTroopsTask(profile, TpDailyTaskEnum.TRAINING_LANCER, TroopType.LANCER),
				    () -> new TrainingTroopsTask(profile, TpDailyTaskEnum.TRAINING_MARKSMAN, TroopType.MARKSMAN)
				));

				taskMappings.put(EnumConfigurationKey.ALLIANCE_PET_TREASURE_BOOL, List.of(
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
				
				taskMappings.put(EnumConfigurationKey.ALLIANCE_AUTOJOIN_BOOL, List.of(
						() -> new AllianceAutojoinTask(profile, TpDailyTaskEnum.ALLIANCE_AUTOJOIN)
					));
				
				taskMappings.put(EnumConfigurationKey.STOREHOUSE_CHEST_BOOL, List.of(
						() -> new OnlineRewardTask(profile, TpDailyTaskEnum.STOREHOUSE_CHEST)
					));
				
				taskMappings.put(EnumConfigurationKey.STOREHOUSE_STAMINA_BOOL, List.of(
						() -> new DailyStaminaTask(profile, TpDailyTaskEnum.STOREHOUSE_STAMINA)
					));
				
				taskMappings.put(EnumConfigurationKey.MAIL_REWARDS_BOOL, List.of(
						() -> new MailRewardsTask(profile, TpDailyTaskEnum.MAIL_REWARDS)
					));
				
				taskMappings.put(EnumConfigurationKey.PET_PERSONAL_TREASURE_BOOL, List.of(
                        () -> new PetAdventureChestTask(profile, TpDailyTaskEnum.PET_ADVENTURE)
                    ));
				
				taskMappings.put(EnumConfigurationKey.GATHER_MEAT_BOOL, List.of(
                        () -> new GatherTask(profile, TpDailyTaskEnum.GATHER_MEAT, GatherType.MEAT)
                    ));
				
				taskMappings.put(EnumConfigurationKey.GATHER_WOOD_BOOL, List.of(
                        () -> new GatherTask(profile, TpDailyTaskEnum.GATHER_WOOD, GatherType.WOOD)
                    ));
				
				taskMappings.put(EnumConfigurationKey.GATHER_COAL_BOOL, List.of(
                        () -> new GatherTask(profile, TpDailyTaskEnum.GATHER_COAL, GatherType.COAL)
                    ));
				
				taskMappings.put(EnumConfigurationKey.GATHER_IRON_BOOL, List.of(
                        () -> new GatherTask(profile, TpDailyTaskEnum.GATHER_IRON, GatherType.IRON)
                    ));
				
				taskMappings.put(EnumConfigurationKey.INTEL_BOOL, List.of(
                        () -> new IntelligenceTask(profile, TpDailyTaskEnum.INTEL)
                    ));
				
				taskMappings.put(EnumConfigurationKey.LIFE_ESSENCE_BOOL, List.of(
                        () -> new LifeEssenceTask(profile, TpDailyTaskEnum.LIFE_ESSENCE)
                    ));
				
				taskMappings.put(EnumConfigurationKey.CITY_UPGRADE_FURNACE_BOOL, List.of(
                        () -> new UpgradeFurnaceTask(profile, TpDailyTaskEnum.CITY_UPGRADE_FURNACE)
                    ));

				//@formatter:on

				// Obtener el estado de las tareas desde la base de datos
				Map<Integer, DTODailyTaskStatus> taskSchedules = iDailyTaskRepository.findDailyTasksStatusByProfile(profile.getId());

				// Recorrer el mapa y programar las tareas según la configuración del perfil
				taskMappings.forEach((key, taskSuppliers) -> {
					if (profile.getConfig(key, Boolean.class)) {
//						ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Scheduling tasks");
						for (Supplier<DelayedTask> taskSupplier : taskSuppliers) {
							DelayedTask task = taskSupplier.get();
							DTOTaskState taskState = new DTOTaskState();
							taskState.setProfileId(profile.getId());
							taskState.setTaskId(task.getTpTask().getId());
							taskState.setExecuting(false);
							taskState.setScheduled(true);
							taskState.setNextExecutionTime(task.getScheduled());
							
							if (taskSchedules.containsKey(task.getTpDailyTaskId())) {
								DTODailyTaskStatus taskStatus = taskSchedules.get(task.getTpDailyTaskId());
								LocalDateTime nextSchedule = taskStatus.getNextSchedule();
								task.reschedule(nextSchedule);
								taskState.setLastExecutionTime(taskStatus.getLastExecution());

								ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Next Execution: " + nextSchedule.format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss")));

							} else {
								ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, task.getTaskName(), profile.getName(), "Task not completed, scheduling for today");
								task.reschedule(LocalDateTime.now());
								taskState.setLastExecutionTime(null); // No previous execution
							}

							ServTaskManager.getInstance().setTaskState(profile.getId(), taskState);
							queue.addTask(task);
						}
					}
				});

			});

			queueManager.startQueues();

			listeners.forEach(e -> {
				DTOBotState state = new DTOBotState();
				state.setRunning(true);
				state.setPaused(false);
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

		listeners.forEach(e -> {
			DTOBotState state = new DTOBotState();
			state.setRunning(false);
			state.setPaused(false);
			state.setActionTime(LocalDateTime.now());
			e.onBotStateChange(state);
		});
	}

	public void pauseBot() {
		queueManager.pauseQueues();

		listeners.forEach(e -> {
			DTOBotState state = new DTOBotState();
			state.setRunning(true);
			state.setPaused(true);
			state.setActionTime(LocalDateTime.now());
			e.onBotStateChange(state);
		});
	}

	public void resumeBot() {
		queueManager.resumeQueues();

		listeners.forEach(e -> {
			DTOBotState state = new DTOBotState();
			state.setRunning(true);
			state.setPaused(false);
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

	public void saveEmulatorPath(String enumConfigurationKey, String filePath) {
		List<Config> configs = iConfigRepository.getGlobalConfigs();

		Config config = configs.stream().filter(c -> c.getKey().equals(enumConfigurationKey)).findFirst().orElse(null);

		if (config == null) {
			TpConfig tpConfig = iConfigRepository.getTpConfig(TpConfigEnum.GLOBAL_CONFIG);
			config = new Config();
			config.setKey(enumConfigurationKey);
			config.setValor(filePath);
			config.setTpConfig(tpConfig);
			iConfigRepository.addConfig(config);
		} else {
			config.setValor(filePath);
			iConfigRepository.saveConfig(config);
		}
	}

	public TaskQueueManager getQueueManager() {
		return queueManager;
	}

}
