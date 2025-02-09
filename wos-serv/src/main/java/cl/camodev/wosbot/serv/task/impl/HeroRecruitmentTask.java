package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class HeroRecruitmentTask extends DelayedTask {

	private Integer DAILY_RECRUITMENT_LIMIT = 5;

	private final String TASK_NAME = "Hero Recruitment Task";

	public HeroRecruitmentTask(String taskName, LocalDateTime scheduledTime) {
		super(taskName, scheduledTime);
	}

	@Override
	protected void execute() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Executing " + TASK_NAME);
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Checking daily recruitment limit");
		if (DAILY_RECRUITMENT_LIMIT > 0) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Reluiting hero");
			DAILY_RECRUITMENT_LIMIT--;
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Requeueing " + TASK_NAME);
			reschedule(LocalDateTime.now().plusSeconds(15));
		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO,
					"Daily recruitment limit reached, removing task from queue");
			setRecurring(false);
		}

	}
}
