package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;
import net.sourceforge.tess4j.TesseractException;

public class StorehouseChest extends DelayedTask {

	public StorehouseChest(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	public EnumStartLocation getRequiredStartLocation() {
		return EnumStartLocation.HOME;
	}

	public static LocalDateTime parseNextReward(String ocrTime) {
		LocalDateTime now = LocalDateTime.now();

		if (ocrTime == null || ocrTime.isEmpty()) {
			return now;
		}

		// Correcting common OCR misreads
		String correctedTime = ocrTime.replaceAll("[Oo]", "0").replaceAll("[lI]", "1").replaceAll("S", "5").replaceAll("[^0-9:]", "");

		try {
			LocalTime parsedTime = LocalTime.parse(correctedTime, DateTimeFormatter.ofPattern("HH:mm:ss"));
			return now.plusHours(parsedTime.getHour()).plusMinutes(parsedTime.getMinute()).plusSeconds(parsedTime.getSecond());
		} catch (DateTimeParseException e) {
			System.err.println("Error parsing time: " + correctedTime);
			return now;
		}
	}

	@Override
	protected void execute() {
		logInfo("Navigating to the Storehouse.");

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
		sleepTask(500);

		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
		sleepTask(500);

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(20, 250), new DTOPoint(200, 280));
		sleepTask(500);

		DTOImageSearchResult researchCenter = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_RESEARCH_CENTER,  90);

		if (researchCenter.isFound()) {
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, researchCenter.getPoint(), researchCenter.getPoint());
			sleepTask(500);
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(30, 430), new DTOPoint(50, 470));
			sleepTask(500);

			DTOImageSearchResult chest = null;
			logInfo("Searching for the storehouse chest.");
			for (int i = 0; i < 5; i++) {
				chest = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_CHEST,  90);

				if (chest.isFound()) {
					// Claim reward, check for stamina and reschedule
					logInfo("Storehouse chest found. Tapping to claim.");
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, chest.getPoint(), chest.getPoint());
					sleepTask(500);

					emuManager.tapBackButton(EMULATOR_NUMBER);
					for (int j = 0; j < 5; j++) {
						DTOImageSearchResult stamina = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_STAMINA, 90);

						if (stamina.isFound()) {
							logInfo("Stamina reward found. Claiming it.");
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, stamina.getPoint(), stamina.getPoint());
							sleepTask(500);
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(250, 930), new DTOPoint(450, 950));
							sleepTask(3000);
							break;
						} else {
							logDebug("Stamina reward not found on this attempt.");
							sleepTask(300);
						}
					}

					// Reschedule based on OCR
					try {
						String nextRewardTime = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(285, 642), new DTOPoint(430, 666));
						LocalDateTime nextReward = parseNextReward(nextRewardTime);
						LocalDateTime reset = UtilTime.getGameReset();

						LocalDateTime scheduledTime = nextReward.isAfter(reset) ? reset : nextReward.minusSeconds(5);

						this.reschedule(scheduledTime);
						ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, scheduledTime);
						logInfo("Storehouse chest claimed. Next check at " + scheduledTime);

					} catch (TesseractException | IOException e) {
						logError("Error during OCR, rescheduling for 5 minutes.", e);
						this.reschedule(LocalDateTime.now().plusMinutes(5));
					}
					
				} else {
					logDebug("Storehouse chest not found on this attempt.");
					sleepTask(300);
				}
			}
			logWarning("Storehouse chest not found after multiple attempts. Rescheduling for 5 minutes.");
			this.reschedule(LocalDateTime.now().plusMinutes(5));
			emuManager.tapBackButton(EMULATOR_NUMBER);

		} else {
			logWarning("Research Center shortcut not found. Rescheduling for 5 minutes.");
			this.reschedule(LocalDateTime.now().plusMinutes(5));
			emuManager.tapBackButton(EMULATOR_NUMBER);
		}
	}

	@Override
	public boolean provideDailyMissionProgress() {return true;}
}
