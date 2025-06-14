package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.sourceforge.tess4j.TesseractException;

public class IntelligenceTask extends DelayedTask {
	boolean intelFound = false;

	private final EmulatorManager emuManager = EmulatorManager.getInstance();

	private final ServLogs servLogs = ServLogs.getServices();

	public IntelligenceTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void execute() {
		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);

		if (homeResult.isFound() || worldResult.isFound()) {
			if (homeResult.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, homeResult.getPoint());
				sleepTask(3000);
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to intelligence");
			}

			DTOImageSearchResult intelligence = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_INTEL.getTemplate(), 0, 0, 720, 1280, 90);
			if (intelligence.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, intelligence.getPoint());
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for completed missions");
				for (int i = 0; i < 5; i++) {
					servLogs.appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), "Searching for completed missions attempt " + i);
					DTOImageSearchResult completed = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_COMPLETED.getTemplate(), 0, 0, 720, 1280, 90);
					if (completed.isFound()) {
						emuManager.tapAtPoint(EMULATOR_NUMBER, completed.getPoint());
						emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(700, 1270), new DTOPoint(710, 1280), 10, 100);
					}
				}
				if (profile.getConfig(EnumConfigurationKey.INTEL_FIRE_BEAST_BOOL, Boolean.class)) {
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for fire beasts");
					if (searchAndProcessBeast(EnumTemplates.INTEL_FIRE_BEAST, 5)) {
						//this.reschedule(LocalDateTime.now());
						//return;
						intelFound = true;
					}
				}
			}

			if (intelligence.isFound()) {
				sleepTask(1000);
				emuManager.tapAtPoint(EMULATOR_NUMBER, intelligence.getPoint());
				if (profile.getConfig(EnumConfigurationKey.INTEL_BEASTS_BOOL, Boolean.class)) {
					// @formatter:off
					List<EnumTemplates> beastPriorities = Arrays.asList(
							EnumTemplates.INTEL_BEAST_YELLOW, 
							EnumTemplates.INTEL_BEAST_PURPLE, 
							EnumTemplates.INTEL_BEAST_BLUE, 
							EnumTemplates.INTEL_BEAST_GREEN, 
							EnumTemplates.INTEL_BEAST_GREY, 
							EnumTemplates.INTEL_PREFC_BEAST_BLUE,
							EnumTemplates.INTEL_PREFC_BEAST_GREEN, 
							EnumTemplates.INTEL_PREFC_BEAST_GREY, 
							EnumTemplates.INTEL_PREFC_BEAST_PURPLE, 
							EnumTemplates.INTEL_PREFC_BEAST_YELLOW);
					// @formatter:on
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for beasts");
					for (EnumTemplates beast : beastPriorities) {
						if (searchAndProcessBeast(beast, 5)) {
							//this.reschedule(LocalDateTime.now());
							//return;
							intelFound = true;
							sleepTask(500);
						}
					}
				}
			}

			if (intelligence.isFound()) {
				sleepTask(1000);
				emuManager.tapAtPoint(EMULATOR_NUMBER, intelligence.getPoint());
				if (profile.getConfig(EnumConfigurationKey.INTEL_CAMP_BOOL, Boolean.class)) {
					// @formatter:off
					List<EnumTemplates> priorities = Arrays.asList(
							EnumTemplates.INTEL_SURVIVOR_YELLOW, 
							EnumTemplates.INTEL_SURVIVOR_PURPLE, 
							EnumTemplates.INTEL_SURVIVOR_BLUE, 
							EnumTemplates.INTEL_SURVIVOR_GREEN, 
							EnumTemplates.INTEL_SURVIVOR_GREY,
							EnumTemplates.INTEL_PREFC_SURVIVOR_YELLOW,
							EnumTemplates.INTEL_PREFC_SURVIVOR_PURPLE,
							EnumTemplates.INTEL_PREFC_SURVIVOR_BLUE,
							EnumTemplates.INTEL_PREFC_SURVIVOR_GREEN,
							EnumTemplates.INTEL_PREFC_SURVIVOR_GREY);
					// @formatter:on
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for survivors");
					for (EnumTemplates beast : priorities) {
						if (searchAndProcessSurvivor(beast, 5)) {
							//this.reschedule(LocalDateTime.now());
							//return;
							intelFound = true;
						}
					}

				}
			}

			if (intelligence.isFound()) {
				sleepTask(1000);
				emuManager.tapAtPoint(EMULATOR_NUMBER, intelligence.getPoint());
				if (profile.getConfig(EnumConfigurationKey.INTEL_EXPLORATION_BOOL, Boolean.class)) {
					// @formatter:off
					List<EnumTemplates> priorities = Arrays.asList(
							EnumTemplates.INTEL_JOURNEY_YELLOW, 
							EnumTemplates.INTEL_JOURNEY_PURPLE, 
							EnumTemplates.INTEL_JOURNEY_BLUE, 
							EnumTemplates.INTEL_JOURNEY_GREEN, 
							EnumTemplates.INTEL_JOURNEY_GREY,
							EnumTemplates.INTEL_PREFC_JOURNEY_YELLOW,
							EnumTemplates.INTEL_PREFC_JOURNEY_PURPLE,
							EnumTemplates.INTEL_PREFC_JOURNEY_BLUE,
							EnumTemplates.INTEL_PREFC_JOURNEY_GREEN,
							EnumTemplates.INTEL_PREFC_JOURNEY_GREY);
					// @formatter:on
					servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Searching for explorations");
					for (EnumTemplates beast : priorities) {
						if (searchAndProcessExploration(beast, 5)) {
							//this.reschedule(LocalDateTime.now());
							//return;
							intelFound = true;
						}
					}

				}
			}

			if (intelligence.isFound()) {
				sleepTask(1000);
				emuManager.tapAtPoint(EMULATOR_NUMBER, intelligence.getPoint());
				sleepTask(500);
				if(intelFound == false) {
					try {
						String rescheduleTime = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(120, 110), new DTOPoint(600, 146));
						LocalDateTime reshchedule = parseAndAddTime(rescheduleTime);
						this.reschedule(reshchedule);
						emuManager.tapBackButton(EMULATOR_NUMBER);
						ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, reshchedule);
					} catch (IOException | TesseractException e) {
						this.reschedule(LocalDateTime.now());
						e.printStackTrace();
					}
				} else {
					this.reschedule(LocalDateTime.now());
				}
			}

		} else {
			emuManager.tapBackButton(EMULATOR_NUMBER);
			reschedule(LocalDateTime.now());

		}

	}

	private boolean searchAndProcessExploration(EnumTemplates exploration, int maxAttempts) {
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), "Searching for " + exploration + " attempt " + attempt);
			DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, exploration.getTemplate(), 0, 0, 720, 1280, 95);

			if (result.isFound()) {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Found :" + exploration);
				processJourney(result);
				return true; // Salir del bucle, bestia encontrada
			}
		}
		return false; // No se encontró la bestia después de maxAttempts intentos
	}

	private void processJourney(DTOImageSearchResult result) {
		emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
		sleepTask(2000);

		DTOImageSearchResult view = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_VIEW.getTemplate(), 0, 0, 720, 1280, 90);
		if (view.isFound()) {
			emuManager.tapAtPoint(EMULATOR_NUMBER, view.getPoint());
			sleepTask(500);
			DTOImageSearchResult explore = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_EXPLORE.getTemplate(), 0, 0, 720, 1280, 90);
			if (explore.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, explore.getPoint());
				sleepTask(500);
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(520, 1200));
				sleepTask(1000);
				emuManager.tapBackButton(EMULATOR_NUMBER);

			}

		}

	}

	private boolean searchAndProcessSurvivor(EnumTemplates survivor, int maxAttempts) {
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), "Searching for " + survivor + " attempt " + attempt);
			DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, survivor.getTemplate(), 0, 0, 720, 1280, 95);

			if (result.isFound()) {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Found :" + survivor);
				processSurvivor(result);
				return true; // Salir del bucle, bestia encontrada
			}
		}
		return false; // No se encontró la bestia después de maxAttempts intentos
	}

	private void processSurvivor(DTOImageSearchResult result) {
		emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
		sleepTask(2000);

		DTOImageSearchResult view = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_VIEW.getTemplate(), 0, 0, 720, 1280, 90);
		if (view.isFound()) {
			emuManager.tapAtPoint(EMULATOR_NUMBER, view.getPoint());
			sleepTask(500);
			DTOImageSearchResult rescue = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_RESCUE.getTemplate(), 0, 0, 720, 1280, 90);
			if (rescue.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, rescue.getPoint());
			}

		}

	}

	private boolean searchAndProcessBeast(EnumTemplates beast, int maxAttempts) {
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.DEBUG, taskName, profile.getName(), "Searching for " + beast + " attempt " + attempt);
			DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, beast.getTemplate(), 0, 0, 720, 1280, 80);

			if (result.isFound()) {
				servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Found :" + beast);
				processBeast(result);
				return true; // Salir del bucle, bestia encontrada
			}
		}
		return false; // No se encontró la bestia después de maxAttempts intentos
	}

	private void processBeast(DTOImageSearchResult beast) {
		emuManager.tapAtPoint(EMULATOR_NUMBER, beast.getPoint());
		sleepTask(2000);

		DTOImageSearchResult view = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_VIEW.getTemplate(), 0, 0, 720, 1280, 90);
		if (view.isFound()) {
			emuManager.tapAtPoint(EMULATOR_NUMBER, view.getPoint());
			sleepTask(500);
			DTOImageSearchResult attack = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_ATTACK.getTemplate(), 0, 0, 720, 1280, 90);
			if (attack.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, attack.getPoint());
				sleepTask(500);
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(198, 1188)); // Click equalize
				sleepTask(500);
				DTOImageSearchResult rally = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_ATTACK_CONFIRM.getTemplate(), 0, 0, 720, 1280, 90);
				if (rally.isFound()) {
					emuManager.tapAtPoint(EMULATOR_NUMBER, rally.getPoint());
				}

			}

		}
	}

	public LocalDateTime parseAndAddTime(String ocrText) {
		// Expresión regular para capturar el tiempo en formato HH:mm:ss
		Pattern pattern = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})");
		Matcher matcher = pattern.matcher(ocrText);

		if (matcher.find()) {
			try {
				int hours = Integer.parseInt(matcher.group(1));
				int minutes = Integer.parseInt(matcher.group(2));
				int seconds = Integer.parseInt(matcher.group(3));

				return LocalDateTime.now().plus(hours, ChronoUnit.HOURS).plus(minutes, ChronoUnit.MINUTES).plus(seconds, ChronoUnit.SECONDS);
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		return LocalDateTime.now();
	}

}
