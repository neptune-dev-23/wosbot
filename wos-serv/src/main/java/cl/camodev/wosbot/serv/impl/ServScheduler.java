package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.TaskQueue;
import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.wosbot.serv.task.impl.CrystalLaboratoryTask;
import cl.camodev.wosbot.serv.task.impl.ExplorationTask;
import cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask;
import cl.camodev.wosbot.serv.task.impl.InitializeTask;
import cl.camodev.wosbot.serv.task.impl.NomadicMerchantTask;
import cl.camodev.wosbot.serv.task.impl.WarAcademyTask;

public class ServScheduler {
	private static ServScheduler instance;

	private final TaskQueueManager queueManager = new TaskQueueManager();

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

			queueManager.startQueue(queueName);
		});
	}

	public void stopBot() {
		queueManager.stopQueues();
	}

}
