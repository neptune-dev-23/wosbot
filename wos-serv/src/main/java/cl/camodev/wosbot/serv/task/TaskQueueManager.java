package cl.camodev.wosbot.serv.task;

import java.util.HashMap;
import java.util.Map;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;

public class TaskQueueManager {

	private final Map<Long, TaskQueue> taskQueues = new HashMap<>();

	public void createQueue(DTOProfiles profile) {
		if (!taskQueues.containsKey(profile.getId())) {
			taskQueues.put(profile.getId(), new TaskQueue(profile));
		}
	}

	public TaskQueue getQueue(Long queueName) {
		return taskQueues.get(queueName);
	}

	public void startQueues() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueueManager", "-", "Starting queues");
		taskQueues.forEach((k, v) -> {
			v.start();
		});
	}

	public void stopQueues() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueueManager", "-", "Stopping queues");
		taskQueues.forEach((k, v) -> {
			v.stop();
		});
		taskQueues.clear();
	}

	public void pauseQueues() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueueManager", "-", "Pausing queues");
		taskQueues.forEach((k, v) -> {
			v.pause();
		});
	}

	public void resumeQueues() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "TaskQueueManager", "-", "Resuming queues");
		taskQueues.forEach((k, v) -> {
			v.resume();
		});
	}

}
