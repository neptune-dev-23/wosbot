package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;

public class StorehouseChest extends DelayedTask {

    private LocalDateTime nextStaminaClaim = LocalDateTime.now();

    public StorehouseChest(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	public EnumStartLocation getRequiredStartLocation() {
		return EnumStartLocation.HOME;
	}

	@Override
	protected void execute() {
		logInfo("Navigating to the Storehouse.");

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
		sleepTask(500);

		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
		sleepTask(700);

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(20, 250), new DTOPoint(200, 280));
		sleepTask(700);

		DTOImageSearchResult researchCenter = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_RESEARCH_CENTER,  90);

		if (researchCenter.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, researchCenter.getPoint(), researchCenter.getPoint());
			sleepTask(1000);
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(30, 430), new DTOPoint(50, 470));
			sleepTask(1000);
            emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1,636), new DTOPoint(2,636),2,300);
			logInfo("Searching for the storehouse chest.");
			for (int i = 0; i < 5; i++) {
				DTOImageSearchResult chest = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_CHEST,  90);

				logDebug("Searching for storehouse chest (Attempt " + (i + 1) + "/5).");
				if (chest.isFound()) {
					// Claim reward, check for stamina and reschedule
					logInfo("Storehouse chest found. Tapping to claim.");
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, chest.getPoint(), chest.getPoint());
					sleepTask(500);
                    emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1,636), new DTOPoint(2,636),5,300);
                    break;
				}else{
                    logDebug("Storehouse chest not found (Attempt " + (i + 1) + "/5).");
                    emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1,636), new DTOPoint(2,636),2,300);
                }
				sleepTask(300);
			}

            // Only search for stamina if current time is >= nextStaminaClaim
            if (!LocalDateTime.now().isBefore(nextStaminaClaim)) {
                logInfo("Searching for stamina rewards.");
                for (int j = 0; j < 10; j++) {
                    DTOImageSearchResult stamina = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_STAMINA, 90);

                    if (stamina.isFound()) {
                        logInfo("Stamina reward found. Claiming it.");
                        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, stamina.getPoint(), stamina.getPoint());
                        sleepTask(500);
                        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(250, 930), new DTOPoint(450, 950));
                        sleepTask(4000);

                        // After successfully claiming stamina, schedule next claim at next reset
                        try {
                            nextStaminaClaim = UtilTime.getNextReset();
                            logInfo("Next stamina claim scheduled at " + nextStaminaClaim);
                        } catch (Exception e) {
                            logDebug("Error obtaining next reset for stamina claim; keeping previous schedule.");
                        }

                        break;
                    }else{
                        logDebug("Stamina reward not found (Attempt " + (j + 1) + "/10).");
                        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1,636), new DTOPoint(2,636),2,300);
                    }
                }
            } else {
                logInfo("Skipping stamina search until " + nextStaminaClaim);
            }

            // Reschedule based on OCR
            try {
                for (int attempt = 0; attempt < 5 ; attempt++) {
                    String nextRewardTime = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(285, 642), new DTOPoint(430, 666));
                    LocalDateTime nextReward = UtilTime.parseTime(nextRewardTime);
                    LocalDateTime nextReset = UtilTime.getNextReset();

                    LocalDateTime scheduledTime;
                    if (!nextReward.isBefore(nextReset)) {
                        scheduledTime = nextReset;
                        logInfo("Next reward time exceeds next reset, scheduling at reset to avoid missing stamina.");
                    } else {
                        scheduledTime = nextReward.minusSeconds(3);
                    }

                    this.reschedule(scheduledTime);
                    ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, scheduledTime);
                    logInfo("Storehouse chest claimed. Next check at " + scheduledTime);

                }

            } catch (Exception e) {
                logError("Error during OCR, rescheduling for 5 minutes.", e);
                this.reschedule(LocalDateTime.now().plusMinutes(5));
            }


		} else {
			logWarning("Research Center shortcut not found. Rescheduling for 5 minutes.");
			this.reschedule(LocalDateTime.now().plusMinutes(5));
			emuManager.tapBackButton(EMULATOR_NUMBER);
		}
	}

	@Override
	public boolean provideDailyMissionProgress() {return true;}
}
