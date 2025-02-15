package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class VipTask extends DelayedTask {

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	private final static String TASK_NAME = "VIP Task";

	public VipTask(DTOProfiles list, LocalDateTime scheduledTime) {
		super(TASK_NAME, scheduledTime);
		this.profile = list;
		this.EMULATOR_NUMBER = list.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {

		DTODailyTaskStatus statusTask = ServScheduler.getServices().getDailyTaskStatus(profile, TpDailyTaskEnum.VIP_POINTS);
		if (statusTask.getFinished()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "This task is already done for today, rescheduling for reset");
			reschedule(UtilTime.getGameReset());
			return;
		}

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "Going to VIP menu");
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(430, 48), new DTOPoint(530, 85));
			sleepTask(3000);

			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(540, 813), new DTOPoint(624, 835), 7, 300);
			sleepTask(5000);

			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(530, 860), new DTOPoint(590, 253), 7, 300);
			sleepTask(3000);

			reschedule(UtilTime.getGameReset());
			ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.VIP_POINTS, true);
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "rescheduled task for tomorrow");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, TASK_NAME, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}

	}

	@Override
	public boolean isDailyTask() {
		return true;
	}

}
