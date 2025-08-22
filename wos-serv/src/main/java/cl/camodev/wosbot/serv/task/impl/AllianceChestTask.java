package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class AllianceChestTask extends DelayedTask {

	public AllianceChestTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {

		logInfo("Going to alliance chest");

		// Go to the alliance chest section
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult allianceChestResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_CHEST_BUTTON.getTemplate(), 90);
		if (!allianceChestResult.isFound()) {

			LocalDateTime nextExecutionTime = LocalDateTime.now()
					.plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class));
			this.reschedule(nextExecutionTime);
			return;
		}

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, allianceChestResult.getPoint(), allianceChestResult.getPoint());
		sleepTask(500);

		
		// Open the loot chests section
		logInfo("Opening loot chests");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(56, 375), new DTOPoint(320, 420));
		sleepTask(500);
		
		// Search for the claim rewards button
		DTOImageSearchResult claimAllButton = emuManager.searchTemplate(EMULATOR_NUMBER,
		EnumTemplates.ALLIANCE_CHEST_CLAIM_ALL_BUTTON.getTemplate(), 98);
		if (claimAllButton.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, claimAllButton.getPoint(), claimAllButton.getPoint(), 2,
			500);
			sleepTask(500);
			
			// Close the window
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(578, 1180), new DTOPoint(641, 1200), 2, 500);
		}
		sleepTask(500);
		
		// Move to alliance gifts section
		logInfo("Opening alliance gifts");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(410, 375), new DTOPoint(626, 420));
		sleepTask(500);
		
		// Search for the claim rewards button
		DTOImageSearchResult claimAllButtonGifts = emuManager.searchTemplate(EMULATOR_NUMBER,
		EnumTemplates.ALLIANCE_CHEST_CLAIM_ALL_BUTTON.getTemplate(), 98);
		if (claimAllButtonGifts.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, claimAllButtonGifts.getPoint(),
			claimAllButtonGifts.getPoint(),
			2, 500);
			sleepTask(500);
			
			// Close the window
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(578, 1180), new DTOPoint(641, 1200), 2, 500);
		} else {
			while (true) {
				DTOImageSearchResult claimButton = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_CHEST_CLAIM_BUTTON.getTemplate(), 90);
				if (claimButton.isFound()) {
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, claimButton.getPoint(), claimButton.getPoint(), 1,
					500);
					sleepTask(500);
				} else {
					break;
				}
			}
		}
		sleepTask(500);

		// Check if honor chest is to be claimed
		boolean honorChestEnabled = profile.getConfig(EnumConfigurationKey.ALLIANCE_HONOR_CHEST_BOOL, Boolean.class);
		
		if (honorChestEnabled) {
			
			// Search for the honor chest
			logInfo("Opening honor chest");
			DTOImageSearchResult honorChest = emuManager.searchTemplate(EMULATOR_NUMBER,
			EnumTemplates.ALLIANCE_HONOR_CHEST.getTemplate(), 90);
			if (honorChest.isFound()) {
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, honorChest.getPoint(), honorChest.getPoint(),
				2, 500);
				sleepTask(500);
				
				// Close the window
				emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(578, 1180), new DTOPoint(641, 1200), 2, 500);
			}
			sleepTask(500);
		}
		
		emuManager.tapBackButton(EMULATOR_NUMBER);
		emuManager.tapBackButton(EMULATOR_NUMBER);
		
		LocalDateTime nextExecutionTime = LocalDateTime.now()
		.plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class));
		this.reschedule(nextExecutionTime);
		logInfo("Alliance chest task completed, next execution scheduled in "
				+ profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class) + " minutes.");

	}
}