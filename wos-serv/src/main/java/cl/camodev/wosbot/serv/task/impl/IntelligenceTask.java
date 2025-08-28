package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;
import net.sourceforge.tess4j.TesseractException;

public class IntelligenceTask extends DelayedTask {

	private boolean marchQueueLimitReached = false;
	private boolean beastMarchSent = false;
	private boolean fcEra = false;

	public IntelligenceTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		logInfo("Starting Intel task.");
		fcEra = profile.getConfig(EnumConfigurationKey.INTEL_FC_ERA_BOOL, Boolean.class);

		boolean intelFound = false;
		boolean nonBeastIntelFound = false;
		marchQueueLimitReached = false;
		beastMarchSent = false;

		DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 90);
		DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);

		if (homeResult.isFound() || worldResult.isFound()) {
			if (homeResult.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, homeResult.getPoint());
				sleepTask(3000);
				logInfo("Navigating to Intel screen.");
			}

			ensureOnIntelScreen();
			logInfo("Searching for completed missions to claim.");
			for (int i = 0; i < 5; i++) {
				logDebug("Searching for completed missions. Attempt " + (i + 1) + ".");
				DTOImageSearchResult completed = emuManager.searchTemplate(EMULATOR_NUMBER,
						EnumTemplates.INTEL_COMPLETED.getTemplate(), 90);
				if (completed.isFound()) {
					emuManager.tapAtPoint(EMULATOR_NUMBER, completed.getPoint());
					emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(700, 1270), new DTOPoint(710, 1280), 10,
							100);
				} else {
					break; // No more completed missions found
				}
			}

			if (profile.getConfig(EnumConfigurationKey.INTEL_FIRE_BEAST_BOOL, Boolean.class)) {
				if (marchQueueLimitReached) {
					logInfo("Skipping fire beast search: march queue is full.");
				} else {
					ensureOnIntelScreen();
					logInfo("Searching for Fire Beasts.");
					if (searchAndProcess(EnumTemplates.INTEL_FIRE_BEAST, 5, 90, this::processBeast)) {
						intelFound = true;
					}
				}
			}

			if (profile.getConfig(EnumConfigurationKey.INTEL_BEASTS_BOOL, Boolean.class)) {
				if (marchQueueLimitReached) {
					logInfo("Skipping beast search: march queue is full.");
				} else {
					ensureOnIntelScreen();
					// @formatter:off

					List<EnumTemplates> beastPriorities=null;
					if (fcEra){
						beastPriorities = Arrays.asList(
								EnumTemplates.INTEL_BEAST_YELLOW,
								EnumTemplates.INTEL_BEAST_PURPLE,
								EnumTemplates.INTEL_BEAST_BLUE);
						logInfo("Searching for beasts (FC era).");
					}else{
						beastPriorities = Arrays.asList(
								EnumTemplates.INTEL_PREFC_BEAST_YELLOW,
								EnumTemplates.INTEL_PREFC_BEAST_PURPLE,
								EnumTemplates.INTEL_PREFC_BEAST_BLUE,
								EnumTemplates.INTEL_PREFC_BEAST_GREEN,
								EnumTemplates.INTEL_PREFC_BEAST_GREY);
						logInfo("Searching for beasts (pre-FC era).");
					}

					// @formatter:on
					logInfo("Searching for standard beasts.");
					for (EnumTemplates beast : beastPriorities) {
						if (marchQueueLimitReached) {
							logInfo("March queue is full. Skipping remaining beast searches.");
							break;
						}
						if (searchAndProcess(beast, 5, 90, this::processBeast)) {
							intelFound = true;
							break;
						}
					}
				}
			}

			if (profile.getConfig(EnumConfigurationKey.INTEL_CAMP_BOOL, Boolean.class)) {
				ensureOnIntelScreen();
				// @formatter:off
				List<EnumTemplates> priorities = null;

				if (fcEra) {
					priorities = Arrays.asList(
							EnumTemplates.INTEL_SURVIVOR_YELLOW,
							EnumTemplates.INTEL_SURVIVOR_PURPLE,
							EnumTemplates.INTEL_SURVIVOR_BLUE);
					logInfo("Searching for survivor camps (FC era).");
				} else {
					priorities = Arrays.asList(
							EnumTemplates.INTEL_PREFC_SURVIVOR_YELLOW,
							EnumTemplates.INTEL_PREFC_SURVIVOR_PURPLE,
							EnumTemplates.INTEL_PREFC_SURVIVOR_BLUE,
							EnumTemplates.INTEL_PREFC_SURVIVOR_GREEN,
							EnumTemplates.INTEL_PREFC_SURVIVOR_GREY);
					logInfo("Searching for survivor camps (pre-FC era).");
				}
				// @formatter:on
				logInfo("Searching for survivor camps.");
				for (EnumTemplates beast : priorities) {
					if (searchAndProcess(beast, 5, 90, this::processSurvivor)) {
						intelFound = true;
						nonBeastIntelFound = true;
						break;
					}
				}

			}

			if (profile.getConfig(EnumConfigurationKey.INTEL_EXPLORATION_BOOL, Boolean.class)) {
				ensureOnIntelScreen();
				// @formatter:off
				List<EnumTemplates> priorities = null;
				if (fcEra) {
					logInfo("Searching for explorations (FC era).");
					priorities = Arrays.asList(
							EnumTemplates.INTEL_JOURNEY_YELLOW,
							EnumTemplates.INTEL_JOURNEY_PURPLE,
							EnumTemplates.INTEL_JOURNEY_BLUE);

				} else {
					logInfo("Searching for explorations (pre-FC era).");
				priorities = Arrays.asList(
						EnumTemplates.INTEL_PREFC_JOURNEY_YELLOW,
						EnumTemplates.INTEL_PREFC_JOURNEY_PURPLE,
						EnumTemplates.INTEL_PREFC_JOURNEY_BLUE,
						EnumTemplates.INTEL_PREFC_JOURNEY_GREEN,
						EnumTemplates.INTEL_PREFC_JOURNEY_GREY);
				}
				logInfo("Searching for exploration journeys.");
				for (EnumTemplates beast : priorities) {
					if (searchAndProcess(beast, 5, 90, this::processJourney)) {
						intelFound = true;
						nonBeastIntelFound = true;
						break;
					}
				}

			}

			sleepTask(500);
			if (intelFound == false) {
				logInfo("No intel items found. Attempting to read the cooldown timer.");
				try {
					String rescheduleTimeStr = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(120, 110), new DTOPoint(600, 146));
					LocalDateTime rescheduleTime = parseAndAddTime(rescheduleTimeStr);
					this.reschedule(rescheduleTime);
					emuManager.tapBackButton(EMULATOR_NUMBER);
					ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, rescheduleTime);
					logInfo("No new intel found. Rescheduling task to run at: " + rescheduleTime);
				} catch (IOException | TesseractException e) {
					this.reschedule(LocalDateTime.now().plusMinutes(5));
					logError("Error reading intel cooldown timer: " + e.getMessage(), e);
				}
			} else if (marchQueueLimitReached && !nonBeastIntelFound && !beastMarchSent) {
				LocalDateTime rescheduleTime = LocalDateTime.now().plusMinutes(5);
				this.reschedule(rescheduleTime);
				ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, rescheduleTime);
				logInfo("March queue is full, and only beasts remain. Rescheduling for 5 minutes at " + rescheduleTime);
			} else if (!beastMarchSent) {
				this.reschedule(LocalDateTime.now());
				logInfo("Intel tasks processed. Rescheduling immediately to check for more.");
			}

		} else {
			logWarning("Not on the home or world screen. Going back to find it.");
			emuManager.tapBackButton(EMULATOR_NUMBER);
			reschedule(LocalDateTime.now());
		}
		logInfo("Intel Task finished.");
	}

	private void ensureOnIntelScreen() {
		sleepTask(500);
		logInfo("Ensuring we are on the intel screen.");

		for (int i = 0; i < 5; i++) {
			DTOImageSearchResult intelScreenResult = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.INTEL_SCREEN.getTemplate(), 90);
			if (intelScreenResult.isFound()) {
				logInfo("Already on the intel screen.");
				return;
			}
			logDebug("Intel screen not found. Attempt " + (i + 1) + "/5. Retrying...");
			sleepTask(300);
		}
		logWarning("Failed to find intel screen after 5 attempts.");

		for (int i = 0; i < 5; i++) {
			DTOImageSearchResult intelligence = emuManager.searchTemplate(EMULATOR_NUMBER,
					EnumTemplates.GAME_HOME_INTEL.getTemplate(), 90);
			if (intelligence.isFound()) {
				logInfo("Intel button found. Tapping to open the intel screen.");
				emuManager.tapAtPoint(EMULATOR_NUMBER, intelligence.getPoint());
				sleepTask(500); // Wait for screen transition
				return; // Success
			}
			logDebug("Intel button not found. Attempt " + (i + 1) + "/5. Retrying...");
			sleepTask(300);
		}
		logWarning("Failed to find the intel button after 5 attempts.");
	}	
	
	private boolean searchAndProcess(EnumTemplates template, int maxAttempts, int confidence, Consumer<DTOImageSearchResult> processMethod) {
		for (int attempt = 0; attempt < maxAttempts; attempt++) {
			logDebug("Searching for template '" + template + "', attempt " + (attempt + 1) + ".");
			DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, template.getTemplate(), confidence);

			if (result.isFound()) {
				logInfo("Template found: " + template);
				processMethod.accept(result);
				return true;
			}
		}
		return false;
	}

	private void processJourney(DTOImageSearchResult result) {
		emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
		sleepTask(2000);

		DTOImageSearchResult view = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_VIEW.getTemplate(),  90);
		if (view.isFound()) {
			emuManager.tapAtPoint(EMULATOR_NUMBER, view.getPoint());
			sleepTask(500);
			DTOImageSearchResult explore = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_EXPLORE.getTemplate(),  90);
			if (explore.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, explore.getPoint());
				sleepTask(500);
				emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(520, 1200));
				sleepTask(1000);
				emuManager.tapBackButton(EMULATOR_NUMBER);
			} else {
				logWarning("Could not find the 'Explore' button for the journey. Going back.");
				emuManager.tapBackButton(EMULATOR_NUMBER); // Back from journey screen
				return;
			}
		}
	}

	private void processSurvivor(DTOImageSearchResult result) {
		emuManager.tapAtPoint(EMULATOR_NUMBER, result.getPoint());
		sleepTask(2000);

		DTOImageSearchResult view = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_VIEW.getTemplate(),  90);
		if (view.isFound()) {
			emuManager.tapAtPoint(EMULATOR_NUMBER, view.getPoint());
			sleepTask(500);
			DTOImageSearchResult rescue = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_RESCUE.getTemplate(),  90);
			if (rescue.isFound()) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, rescue.getPoint());
			} else {
				logWarning("Could not find the 'Rescue' button for the survivor. Going back.");
				emuManager.tapBackButton(EMULATOR_NUMBER); // Back from survivor screen
				return;
			}
		}
	}

	private void processBeast(DTOImageSearchResult beast) {
		emuManager.tapAtPoint(EMULATOR_NUMBER, beast.getPoint());
		sleepTask(2000);

		DTOImageSearchResult view = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_VIEW.getTemplate(),  90);
		if (!view.isFound()) {
			logWarning("Could not find the 'View' button for the beast. Going back.");
			emuManager.tapBackButton(EMULATOR_NUMBER);
			return;
		}
		emuManager.tapAtPoint(EMULATOR_NUMBER, view.getPoint());
		sleepTask(500);

		DTOImageSearchResult attack = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.INTEL_ATTACK.getTemplate(),  90);
		if (!attack.isFound()) {
			logWarning("Could not find the 'Attack' button for the beast. Going back.");
			emuManager.tapBackButton(EMULATOR_NUMBER);
			return;
		}
		emuManager.tapAtPoint(EMULATOR_NUMBER, attack.getPoint());
		sleepTask(500);

		// Check if the march screen is open before proceeding
		DTOImageSearchResult deployButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DEPLOY_BUTTON.getTemplate(),  90);
		if (!deployButton.isFound()) {
			// March queue limit reached, cannot process beast
			logError("March queue is full. Cannot start a new march.");
			marchQueueLimitReached = true;
			return;
		}

		boolean useFlag = profile.getConfig(EnumConfigurationKey.INTEL_USE_FLAG_BOOL, Boolean.class);
		if (useFlag) {
			// Select the specified flag
			int flagToSelect = profile.getConfig(EnumConfigurationKey.INTEL_BEASTS_FLAG_INT, Integer.class);
			selectMarchFlag(flagToSelect);
			sleepTask(500);
		}

		DTOImageSearchResult equalizeButton = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.RALLY_EQUALIZE_BUTTON.getTemplate(),  90);

		if (equalizeButton.isFound()) {
			emuManager.tapAtPoint(EMULATOR_NUMBER, equalizeButton.getPoint());
		} else {
			emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(198, 1188));
			sleepTask(500);
		}

		try {
			String timeStr = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(521, 1141), new DTOPoint(608, 1162));
			long travelTimeSeconds = parseTimeToSeconds(timeStr);

			if (travelTimeSeconds > 0) {
				emuManager.tapAtPoint(EMULATOR_NUMBER, deployButton.getPoint());
				sleepTask(1000); // Wait for march to start
				long returnTimeSeconds = (travelTimeSeconds * 2) + 2;
				LocalDateTime rescheduleTime = LocalDateTime.now().plusSeconds(returnTimeSeconds);
				this.reschedule(rescheduleTime);
				ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, rescheduleTime);
				logInfo("Beast march sent. Task will run again at " + rescheduleTime + ".");
				beastMarchSent = true;
			} else {
				logError("Failed to parse march time. Aborting attack.");
				emuManager.tapBackButton(EMULATOR_NUMBER); // Go back from march screen
				sleepTask(500);
				emuManager.tapBackButton(EMULATOR_NUMBER); // Go back from beast screen
			}
		} catch (IOException | TesseractException e) {
			logError("Failed to read march time using OCR. Aborting attack. Error: " + e.getMessage(), e);
			emuManager.tapBackButton(EMULATOR_NUMBER); // Go back from march screen
			sleepTask(500);
			emuManager.tapBackButton(EMULATOR_NUMBER); // Go back from beast screen
		}
	}

	private void selectMarchFlag(int flagNumber) {
        logInfo("Selecting march flag " + flagNumber + ".");
        DTOPoint flagPoint = null;
        switch (flagNumber) {
            case 1: flagPoint = new DTOPoint(70, 120); break;
            case 2: flagPoint = new DTOPoint(140, 120); break;
            case 3: flagPoint = new DTOPoint(210, 120); break;
            case 4: flagPoint = new DTOPoint(280, 120); break;
            case 5: flagPoint = new DTOPoint(350, 120); break;
            case 6: flagPoint = new DTOPoint(420, 120); break;
            case 7: flagPoint = new DTOPoint(490, 120); break;
            case 8: flagPoint = new DTOPoint(560, 160); break;
            default:
                logError("Invalid flag number: " + flagNumber + ". Defaulting to flag 1.");
                flagPoint = new DTOPoint(70, 120);
                break;
        }
        emuManager.tapAtPoint(EMULATOR_NUMBER, flagPoint);
    }

	private long parseTimeToSeconds(String timeString) {
		if (timeString == null || timeString.trim().isEmpty()) {
			return 0;
		}
		timeString = timeString.replaceAll("[^\\d:]", ""); // Clean non-digit/colon characters
		String[] parts = timeString.trim().split(":");
		long seconds = 0;
		try {
			if (parts.length == 2) { // mm:ss
				seconds = Integer.parseInt(parts[0]) * 60L + Integer.parseInt(parts[1]);
			} else if (parts.length == 3) { // HH:mm:ss
				seconds = Integer.parseInt(parts[0]) * 3600L + Integer.parseInt(parts[1]) * 60L + Integer.parseInt(parts[2]);
			}
		} catch (NumberFormatException e) {
			logError("Could not parse time string: " + timeString);
			return 0;
		}
		return seconds;
	}

	public LocalDateTime parseAndAddTime(String ocrText) {
		// Regular expression to capture time in HH:mm:ss format
		Pattern pattern = Pattern.compile("(\\d{1,2}):(\\d{1,2}):(\\d{1,2})");
		Matcher matcher = pattern.matcher(ocrText);

		if (matcher.find()) {
			try {
				int hours = Integer.parseInt(matcher.group(1));
				int minutes = Integer.parseInt(matcher.group(2));
				int seconds = Integer.parseInt(matcher.group(3));

				return LocalDateTime.now().plus(hours, ChronoUnit.HOURS).plus(minutes, ChronoUnit.MINUTES).plus(seconds, ChronoUnit.SECONDS);
			} catch (NumberFormatException e) {
				logError("Error parsing time from OCR text: '" + ocrText + "'", e);
			}
		}

        return LocalDateTime.now().plusMinutes(1); // Default to 1 minute if parsing fails
	}

}
