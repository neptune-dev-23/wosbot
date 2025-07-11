package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

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

public class UpgradeFurnaceTask extends DelayedTask {


	public UpgradeFurnaceTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {
		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);

		if (homeResult.isFound() || worldResult.isFound()) {
			if (worldResult.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, worldResult.getPoint());
				sleepTask(3000);
			}

			servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to check current building queue status");

			// left menu
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
			sleepTask(500);

			// city tab
			emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(20, 250), new DTOPoint(200, 280));
			sleepTask(500);

			// ocr to check current building queue status

			final int MAX_ATTEMPTS = 10;
			boolean success = false;
			LocalDateTime upgradeTime = null;

			try {
				for (int i = 0; i < MAX_ATTEMPTS; i++) {
					try {
						String rawText = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(162, 378), new DTOPoint(293, 397));
						if (rawText.contains("Idle")) {
							break;
						}

						// Sanitizar texto OCR
						String cleanedText = rawText.replaceAll("[^0-9:\\n\\r]", "") // Eliminar caracteres no esperados excepto dígitos y separadores de tiempo
								.replaceAll("[\\n\\r]+", "") // Eliminar saltos de línea
								.trim();

						// Intentar parsear
						// Parsear a LocalTime como duración
						LocalTime parsedTime = LocalTime.parse(cleanedText, DateTimeFormatter.ofPattern("HH:mm:ss"));

						// Convertir a segundos totales
						long totalSeconds = parsedTime.toSecondOfDay();

						// Calcular la mitad del tiempo
						long halfSeconds = totalSeconds / 2;

						// Sumar la mitad al tiempo actual
						upgradeTime = LocalDateTime.now().plusSeconds(halfSeconds);

						success = true;
						break;
					} catch (Exception e) {
						// Opcional: loggear intento fallido
						// System.out.println("OCR parsing failed at attempt " + (i + 1));
					}
				}

				if (success) {
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Queue is already busy");
					reschedule(upgradeTime);
					return;
				} else {

					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "No upgrades in progress, going to upgrade furnace");
					// going to check current furnace requierements
					// survivor status
					emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(320, 30));
					sleepTask(500);

					// search for cookhouse
					DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_CITY_STATUS_COOKHOUSE.getTemplate(), 0, 0, 720, 1280, 90);
					if (result.isFound()) {
						// click on cookhouse
						emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
						sleepTask(500);

						// search go button
						DTOImageSearchResult goButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_CITY_STATUS_GO_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
						if (goButton.isFound()) {
							// click on go button
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, goButton.getPoint(), goButton.getPoint());
							sleepTask(500);

							// go to furnace
							emuManager.tapBackButton(EMULATOR_NUMBER);
							sleepTask(500);
							emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(130, 650));
							sleepTask(1000);

							// go to upgrade button
							emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(608, 715), new DTOPoint(625, 728));
							sleepTask(500);

							// search go button for upgrade pending requirements

							DTOImageSearchResult upgradeGoButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_CITY_STATUS_GO_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);

							if (upgradeGoButton.isFound()) {
								servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Pending upgrade requirements found, going to upgrade requierements");
								// click on go button
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, upgradeGoButton.getPoint(), upgradeGoButton.getPoint());
								sleepTask(1000);
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(330, 729), new DTOPoint(364, 731), 10, 10);

								DTOImageSearchResult upgradeButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_UPGRADE.getTemplate(), 0, 0, 720, 1280, 90);
								if (upgradeButton.isFound()) {
									// click on upgrade button
									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, upgradeButton.getPoint(), upgradeButton.getPoint());
									sleepTask(1000);

									while ((result = emuManager.searchTemplate(EMULATOR_NUMBER,	EnumTemplates.GAME_HOME_SHORTCUTS_OBTAIN.getTemplate(), 0, 0, 720, 1280, 90)).isFound()) {
										logInfo("Refilling resources for upgrade");
										emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
										sleepTask(500);
										// click replenish button
										emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(358,1135));
										sleepTask(300);

										// confirm replenish
										emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(511, 1056));
										sleepTask(1000);
									}

									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(489, 1034), new DTOPoint(500, 1050));

								} else {
									servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Upgrade button not found, skipping upgrade");
									reschedule(LocalDateTime.now());
									return;
								}

								for (int i = 0; i < MAX_ATTEMPTS; i++) {
									DTOImageSearchResult alliesHelpButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_HELP_REQUEST.getTemplate(), 0, 0, 720, 1280, 90);

									if (alliesHelpButton.isFound()) {
										servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Allies help button found, requesting help");
										emuManager.tapAtRandomPoint(EMULATOR_NUMBER, alliesHelpButton.getPoint(), alliesHelpButton.getPoint());
										sleepTask(300);
										break;
									} else if (i == MAX_ATTEMPTS - 1) {
										servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Allies help button not found, skipping request");
										reschedule(LocalDateTime.now());
										return;
									}
								}

							} else {
								// click on upgrade button


								while ((result = emuManager.searchTemplate(EMULATOR_NUMBER,	EnumTemplates.GAME_HOME_SHORTCUTS_OBTAIN.getTemplate(), 0, 0, 720, 1280, 90)).isFound()) {
									logInfo("Refilling resources for upgrade");
									emuManager.tapAtRandomPoint(EMULATOR_NUMBER, result.getPoint(), result.getPoint());
									sleepTask(500);
									// click replenish button
									emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(358,1135));
									sleepTask(300);

									// confirm replenish
									emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(511, 1056));
									sleepTask(1000);
								}

								servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "No pending upgrade requirements found for furnace, upgrading furnace directly");
								emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(470, 1196), new DTOPoint(480, 1216));


								sleepTask(1000);

								// check if allies can help

								for (int i = 0; i < MAX_ATTEMPTS; i++) {
									DTOImageSearchResult alliesHelpButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_HELP_REQUEST.getTemplate(), 0, 0, 720, 1280, 90);

									if (alliesHelpButton.isFound()) {
										servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Allies help button found, requesting help");
										emuManager.tapAtRandomPoint(EMULATOR_NUMBER, alliesHelpButton.getPoint(), alliesHelpButton.getPoint());
										sleepTask(500);
										break;
									} else if (i == MAX_ATTEMPTS - 1) {
										servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Allies help button not found, skipping request");
									}
								}
								reschedule(LocalDateTime.now());
								return;

							}
							this.reschedule(LocalDateTime.now());
						} else {
							servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Go button not found for cookhouse upgrade");
						}
					}

				}


			} catch (Exception e) {
				// TODO: handle exception
			}

		} else{
			emuManager.tapBackButton(EMULATOR_NUMBER);
			this.reschedule(LocalDateTime.now());
		}

	}
}
