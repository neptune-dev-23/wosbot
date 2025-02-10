package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.serv.task.DelayedTask;

public class HeroRecruitmentTask extends DelayedTask {

	private final String TASK_NAME = "Hero Recruitment Task";

	public HeroRecruitmentTask(String taskName, LocalDateTime scheduledTime) {
		super(scheduledTime);
	}

	@Override
	protected void execute() {

	}
}
