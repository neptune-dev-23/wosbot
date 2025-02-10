package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;
import java.util.List;

import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.wosbot.serv.task.impl.NomadicMerchantTask;

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
					EmulatorManager.getInstance().initializeAdbConnection(profile.getEmulatorNumber().toString());
					String queueName = profile.getName();
					queueManager.createQueue(queueName);
					queueManager.getQueue(queueName).addTask(new NomadicMerchantTask(profile, LocalDateTime.now()));
					queueManager.startQueue(queueName);

				}
			});
		}

	}

	public void stopBot() {
		EmulatorManager.getInstance().closeAllAdbConnections();

	}

}
