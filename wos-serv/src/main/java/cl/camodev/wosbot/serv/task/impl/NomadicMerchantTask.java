package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.Task;

public class NomadicMerchantTask extends Task {

	public NomadicMerchantTask(String profile, LocalDateTime scheduledTime) {
		super(profile, scheduledTime);
	}

	@Override
	protected void execute() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "["+taskName + "] checking for nomadic merchant offers");
		reschedule(LocalDateTime.now().plusMinutes(2));
	}
}
