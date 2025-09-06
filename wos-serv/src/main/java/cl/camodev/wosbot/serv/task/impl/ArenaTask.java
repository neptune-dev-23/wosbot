package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class ArenaTask extends DelayedTask {

	public ArenaTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		logInfo("Starting arena task.");
		logWarning("Arena task is not yet implemented.");
	}


	@Override
	public boolean provideDailyMissionProgress() {return true;}
}
