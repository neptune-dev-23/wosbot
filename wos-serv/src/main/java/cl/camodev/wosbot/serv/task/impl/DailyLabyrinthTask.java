package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class DailyLabyrinthTask extends DelayedTask {

	public DailyLabyrinthTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		// Implement the logic for the Daily Labyrinth task here
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Executing daily labyrinth task");
	}

}
