package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class MailRewardsTask extends DelayedTask {

	private final DTOPoint[] buttons = { new DTOPoint(230, 120), new DTOPoint(360, 120), new DTOPoint(500, 120) };

	public MailRewardsTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.GAME_HOME_FURNACE, 90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.GAME_HOME_WORLD, 90);

		if (homeResult.isFound() || worldResult.isFound()) {
			sleepTask(1000);
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to mail");
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(640, 1033),
					new DTOPoint(686, 1064));
			sleepTask(1000);
			for (DTOPoint button : buttons) {
				// Change tabs
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, button, button);
				sleepTask(1000);

				// Claim rewards
				logInfo("Claiming rewards");
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(420, 1227),
						new DTOPoint(450, 1250), 2, 500);
				sleepTask(500);

				// Check if there are excess unread mail
				int searchAttempts = 0;
				while (true) {
					DTOImageSearchResult unclaimedRewards = emuManager.searchTemplate(EMULATOR_NUMBER,
							EnumTemplates.MAIL_UNCLAIMED_REWARDS, 90);
					if (unclaimedRewards.isFound()) {
						
						if(searchAttempts > 0) {
							logInfo("Excess unread mail found, swiping down and claiming");
							
							// Swipe down 10 times
							for (int i = 0; i < 10; i++) {
								emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(40, 913), new DTOPoint(40, 400));
								sleepTask(300);
							}
						}

						// Claim rewards
						emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(420, 1227),
								new DTOPoint(450, 1250), 3, 1000);
						sleepTask(500);

						searchAttempts++;
					} else {
						break;
					}
					sleepTask(500);
				}
			}
			LocalDateTime nextSchedule = LocalDateTime.now()
					.plusMinutes(profile.getConfig(EnumConfigurationKey.MAIL_REWARDS_OFFSET_INT, Integer.class));
			this.reschedule(nextSchedule);
			ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, nextSchedule);
			emuManager.tapBackButton(EMULATOR_NUMBER);

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(),
					"Home not found");
			emuManager.tapBackButton(EMULATOR_NUMBER);

		}
	}

}