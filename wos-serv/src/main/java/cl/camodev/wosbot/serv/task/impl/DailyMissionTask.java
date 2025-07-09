package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class DailyMissionTask extends DelayedTask {

	private EmulatorManager emulatorManager = EmulatorManager.getInstance();
	private ServScheduler servScheduler = ServScheduler.getServices();
	private ServLogs servLogs = ServLogs.getServices();

	public DailyMissionTask(DTOProfiles profile, TpDailyTaskEnum dailyMission) {
		super(profile, dailyMission);
	}

	@Override
	protected void execute() {

		emulatorManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(50, 1050));
		sleepTask(2000);

		DTOImageSearchResult result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DAILY_MISSION_DAILY_TAB.getTemplate(), 0, 0, 720, 1280, 90);

		if (result.isFound()) {
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Switching to Daily Mission Tab");
			emulatorManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
			sleepTask(500);
		}

		servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for complete button");
		result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DAILY_MISSION_CLAIMALL_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);

		if (result.isFound()) {
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Claiming Daily Mission Reward");
			emulatorManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
			emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(10, 100), new DTOPoint(600, 120), 20, 50);
		} else {
			servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Complete button not found, trying to each mission");
			while (emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DAILY_MISSION_CLAIM_BUTTON.getTemplate(), 0, 0, 720, 1280, 90).isFound()) {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Claim button found, tapping on it");
				emulatorManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
				emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(10, 100), new DTOPoint(600, 120), 20, 50);
				sleepTask(500);
			}
		}
		emulatorManager.tapBackButton(EMULATOR_NUMBER);
		sleepTask(50);

	}

}
