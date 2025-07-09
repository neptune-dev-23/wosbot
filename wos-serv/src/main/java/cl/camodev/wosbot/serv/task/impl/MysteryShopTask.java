package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class MysteryShopTask extends DelayedTask {

	public MysteryShopTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		// Implement the logic for the Mystery Shop task here
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Executing mystery shop task");
	}

}
