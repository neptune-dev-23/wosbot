package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.serv.task.DelayedTask;

public class HeroRecruitmentTask extends DelayedTask {
	public HeroRecruitmentTask(String taskName, LocalDateTime scheduledTime) {
		super(taskName, scheduledTime);
	}

	@Override
	protected void execute() {
		System.out.println("Ejecutando " + taskName + " a las " + LocalDateTime.now());
		reschedule(LocalDateTime.now().plusSeconds(15));
		setRecurring(false);
	}
}
