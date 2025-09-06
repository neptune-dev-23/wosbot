package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;

public class TriumphTask extends DelayedTask {

	public TriumphTask(DTOProfiles profile, TpDailyTaskEnum dailyMission) {
		super(profile, dailyMission);
	}

	@Override
	public EnumStartLocation getRequiredStartLocation() {
		return EnumStartLocation.HOME;
	}

	@Override
	protected void execute() {
		logInfo("Navigating to the Alliance Menu to claim Triumph rewards.");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_TRIUMPH_BUTTON.getTemplate(),  90);
		if (result.isFound()) {
			logInfo("Alliance Triumph button found. Tapping to open the menu.");
			emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
			sleepTask(2000);

			logInfo("Verifying if Triumph rewards have already been claimed.");
			// verify if its already claimed daily
			result = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.ALLIANCE_TRIUMPH_DAILY_CLAIMED.getTemplate(),  90);

			if (result.isFound()) {
				logInfo("Daily Triumph rewards have already been claimed. Rescheduling for the next game reset.");
				this.reschedule(UtilTime.getGameReset());
			} else {
				// verify if its ready to claim
				logInfo("Daily Triumph rewards have not been claimed yet. Checking if they are ready to be claimed.");
				result = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_TRIUMPH_DAILY.getTemplate(),
						 90);
				if (result.isFound()) {
					logInfo("Daily Triumph rewards are ready. Tapping to claim.");
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint(), 10, 50);
					reschedule(UtilTime.getGameReset());
				} else {
					// not ready, reschedule for next schedule using offset configuration
					logWarning("Daily Triumph rewards are not ready to be claimed. Rescheduling based on the configured offset.");

					reschedule(LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_TRIUMPH_OFFSET_INT, Integer.class)));

				}

			}

			// verify if can claim weekly

//			result = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_TRIUMPH_WEEKLY.getTemplate(),  90);
//
//			if (result.isFound()) {
//				emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint(), 50, 10);
//				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Weekly Triumph claimed successfully.");
//			} else {
//				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Weekly Triumph not ready to claim");
//			}

		} else {
			logError("Alliance Triumph button not found. Unable to claim rewards. Rescheduling based on the configured offset.");

			reschedule(LocalDateTime.now().plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_TRIUMPH_OFFSET_INT, Integer.class)));
		}

	}

}
