package cl.camodev.wosbot.serv.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.serv.task.TaskQueueManager;
import cl.camodev.wosbot.serv.task.impl.HeroRecruitmentTask;

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

		String queueName = "VICI_0";
		queueManager.createQueue(queueName);
		queueManager.getQueue(queueName).addTask(new HeroRecruitmentTask(queueName, LocalDateTime.now()));
		queueManager.startQueue(queueName);

//		configs.forEach(config -> {
//			
//		});

//		for (DTOConfig config : configs) {
//			String queueName = config.getProfileName() + "_" + config.getEmulatorNumber();
//
//			queueManager.createQueue(queueName);
//			queueManager.getQueue(queueName).addTask(new InitializeTask(queueName));
//
//			if (config.getNomadicMerchant().booleanValue()) {
//				NomadicMerchantTask nomadicMerchantTask = new NomadicMerchantTask(queueName);
//				queueManager.getQueue(queueName).addTask(nomadicMerchantTask);
//			}
//
//			queueManager.startQueue(queueName);
//		}

	}

	public void stopBot() {
		// TODO Auto-generated method stub

	}

}
