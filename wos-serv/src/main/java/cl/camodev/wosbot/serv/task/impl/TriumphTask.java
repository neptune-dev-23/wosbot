package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
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

public class TriumphTask extends DelayedTask {

	private EmulatorManager emulatorManager = EmulatorManager.getInstance();
	private ServScheduler servScheduler = ServScheduler.getServices();
	private ServLogs servLogs = ServLogs.getServices();

	public TriumphTask(DTOProfiles profile, TpDailyTaskEnum dailyMission) {
		super(profile, dailyMission);
	}

	@Override
	protected void execute() {
		servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to Alliance Menu to claim Triumph rewards");
		emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_TRIUMPH_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
		if (result.isFound()) {
			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Alliance Triumph button found, tapping to open menu");
			emulatorManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
			sleepTask(2000);

			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Verifying if Triumph rewards are already claimed");
			// verify if its already claimed daily
			result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_TRIUMPH_DAILY_CLAIMED.getTemplate(), 0, 0, 720, 1280, 90);

			if (result.isFound()) {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Daily Triumph already claimed, rescheduling for next reset time");
				this.reschedule(UtilTime.getNextReset());
				servScheduler.updateDailyTaskStatus(profile, tpTask, UtilTime.getNextReset());
			} else {
				// verify if its ready to claim
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Daily Triumph not claimed yet, checking if ready to claim");
				result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_TRIUMPH_DAILY.getTemplate(), 0, 0, 720, 1280, 90);
				if (result.isFound()) {
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Daily Triumph ready to claim, tapping to claim rewards");
					emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint(), 50, 10);
				} else {
					// not ready, reschedule for next schedule using offset configuration
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Daily Triumph not ready to claim, rescheduling");
					int offset = profile.getConfig(EnumConfigurationKey.ALLIANCE_TRIUMPH_OFFSET_INT, Integer.class);
					LocalDateTime nextSchedule = LocalDateTime.now().plusHours(offset);
					this.reschedule(nextSchedule);
					servScheduler.updateDailyTaskStatus(profile, tpTask, nextSchedule);
				}

			}

			// verify if can claim weekly

//			result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_TRIUMPH_WEEKLY.getTemplate(), 0, 0, 720, 1280, 90);
//
//			if (result.isFound()) {
//				emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint(), 50, 10);
//				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Weekly Triumph claimed successfully.");
//			} else {
//				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Weekly Triumph not ready to claim");
//			}

		}

	}

}
