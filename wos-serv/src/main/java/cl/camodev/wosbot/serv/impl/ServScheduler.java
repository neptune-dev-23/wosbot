package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.util.List;

import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.wosbot.serv.task.impl.CrystalLaboratoryTask;
import cl.camodev.wosbot.serv.task.impl.ExplorationTask;
import cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask;
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

		if (profiles != null) {
			profiles.forEach(profile -> {
				if (profile.getEnabled()) {
					String queueName = profile.getName();
					EmulatorManager.getInstance().connectADB(profile.getEmulatorNumber().toString());
					queueManager.createQueue(queueName);
					queueManager.getQueue(queueName).addTask(new NomadicMerchantTask(profile, LocalDateTime.now()));
					queueManager.getQueue(queueName).addTask(new ExplorationTask(profile, LocalDateTime.now()));
					queueManager.getQueue(queueName).addTask(new HeroRecruitmentTask(profile, LocalDateTime.now()));
					queueManager.getQueue(queueName).addTask(new CrystalLaboratoryTask(profile, LocalDateTime.now()));
					queueManager.getQueue(queueName).addTask(new WarAcademyTask(profile, LocalDateTime.now()));
					queueManager.startQueue(queueName);

				}
			});
		}

	}

	public void stopBot() {

	}

}
