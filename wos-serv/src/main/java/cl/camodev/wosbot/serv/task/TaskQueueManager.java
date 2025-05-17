package cl.camodev.wosbot.serv.task;

import java.util.HashMap;
import java.util.Map;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;

public class TaskQueueManager {

	private final Map<String, TaskQueue> taskQueues = new HashMap<>();

	public void createQueue(DTOProfiles profile) {
		if (!taskQueues.containsKey(profile.getName())) {
			taskQueues.put(profile.getName(), new TaskQueue(profile));
		}
	}

	public TaskQueue getQueue(String queueName) {
		return taskQueues.get(queueName);
	}

	public void startQueue(String queueName) {
		TaskQueue queue = taskQueues.get(queueName);
		if (queue != null) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueueManager", queueName, "Starting queue for profile");
			queue.start();
		}
	}

	public void stopQueues() {
		taskQueues.forEach((k, v) -> {
			v.stop();
		});
	}

}
