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
import net.sourceforge.tess4j.TesseractException;

public class StorehouseChest extends DelayedTask {

	public StorehouseChest(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
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
		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(),  90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(),  90);

		if (homeResult.isFound() || worldResult.isFound()) {
			if (worldResult.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, worldResult.getPoint());
				sleepTask(3000);
				logInfo("Navigating to the Storehouse.");
			}

			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
			sleepTask(500);

			emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
			sleepTask(500);

			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(20, 250), new DTOPoint(200, 280));
			sleepTask(500);

			DTOImageSearchResult researchCenter = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_RESEARCH_CENTER.getTemplate(),  90);

			if (researchCenter.isFound()) {
				{
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, researchCenter.getPoint(), researchCenter.getPoint());
					sleepTask(500);
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(30, 430), new DTOPoint(50, 470));
					sleepTask(500);

					DTOImageSearchResult chest = null;
					logInfo("Searching for the storehouse chest.");
					for (int i = 0; i < 5; i++) {
						chest = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_CHEST.getTemplate(),  90);

						if (chest.isFound()) {
							// Claim reward, check for stamina and reschedule
							logInfo("Storehouse chest found. Tapping to claim.");
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, chest.getPoint(), chest.getPoint());
							sleepTask(500);

							emuManager.tapBackButton(EMULATOR_NUMBER);
							for (int j = 0; j < 5; j++) {
								DTOImageSearchResult stamina = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_STAMINA.getTemplate(), 90);

								if (stamina.isFound()) {
									logInfo("Stamina reward found. Claiming it.");
									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, stamina.getPoint(), stamina.getPoint());
									sleepTask(500);
									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(250, 930), new DTOPoint(450, 950));
									sleepTask(3000);
									break;
								} else {
									logDebug("Stamina reward not found on this attempt.");
									sleepTask(100);
								}
							}

							break;
						} else {
							logDebug("Storehouse chest not found on this attempt.");
							sleepTask(100);
						}

					}

					if (!chest.isFound()) {
						logInfo("Storehouse chest not found after multiple attempts. Checking for stamina separately.");
						for (int i = 0; i < 5; i++) {
							DTOImageSearchResult stamina = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.STOREHOUSE_STAMINA.getTemplate(),  90);

							if (stamina.isFound()) {
								logInfo("Stamina reward found. Claiming it.");
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, stamina.getPoint(), stamina.getPoint());
								sleepTask(500);
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(250, 930), new DTOPoint(450, 950));
								sleepTask(3000);
								break;
							} else {
								logDebug("Stamina reward not found on this attempt.");
								sleepTask(100);
							}
						}
					}

					// Do OCR to find next reward time and reschedule
					try {
						String nextRewardTime = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(285, 642), new DTOPoint(430, 666));
						logInfo("Next reward cooldown from OCR: " + nextRewardTime);
						LocalDateTime nextReward = parseNextReward(nextRewardTime);
						LocalDateTime reset = UtilTime.getGameReset();

						LocalDateTime scheduledTime = nextReward.isAfter(reset) ? reset : nextReward.minusSeconds(5);

						this.reschedule(scheduledTime);
						ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, scheduledTime);
						logInfo("Storehouse task rescheduled for " + scheduledTime);

					} catch (IOException | TesseractException e) {
						logError("Error during OCR for the next reward time. Rescheduling for 5 minutes.", e);
                        this.reschedule(LocalDateTime.now().plusMinutes(5));
					}

				}
			}

		} else {
			emuManager.tapBackButton(EMULATOR_NUMBER);
		}
	}

	@Override
	public boolean provideDailyMissionProgress() {return true;}
}
