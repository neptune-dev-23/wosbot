package cl.camodev.wosbot.serv.task.impl;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;

/**
 * Task responsible for managing arena challenges.
 * It navigates to the arena, checks available attempts, and challenges opponents with lower power.
 * The task can be configured to buy extra attempts and refresh the opponent list using gems.
 * Activation hour can be set to control when the task runs daily.
 */
public class ArenaTask extends DelayedTask {
	private static final DTOPoint MY_POWER_TOP_LEFT = new DTOPoint(368, 247);
	private static final DTOPoint MY_POWER_BOTTOM_RIGHT = new DTOPoint(564, 284);
	private static final DTOPoint CHALLENGES_LEFT_TOP_LEFT = new DTOPoint(405, 951);
	private static final DTOPoint CHALLENGES_LEFT_BOTTOM_RIGHT = new DTOPoint(439, 986);
	// Activation time in "HH:mm" format (24-hour clock)
	private String activationHour = profile.getConfig(EnumConfigurationKey.ARENA_TASK_ACTIVATION_HOUR_STRING, String.class);
	private int extraAttempts = profile.getConfig(EnumConfigurationKey.ARENA_TASK_EXTRA_ATTEMPTS_INT, Integer.class);
	private boolean refreshWithGems = profile.getConfig(EnumConfigurationKey.ARENA_TASK_REFRESH_WITH_GEMS_BOOL, Boolean.class);
	private int attempts = 0;
    private boolean firstRun = false;

    public ArenaTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);

		// Only schedule if the task is enabled and activation hour is valid
		boolean isTaskEnabled = profile.getConfig(EnumConfigurationKey.ARENA_TASK_BOOL, Boolean.class);
		if (isTaskEnabled && isValidTimeFormat(activationHour)) {
			scheduleActivationTime();
		}
    }

    /**
     * Validates if the given time string is in valid HH:mm format (24-hour clock)
     */
    private boolean isValidTimeFormat(String time) {
        if (time == null || time.trim().isEmpty()) {
            return false;
        }
        try {
            String[] parts = time.split(":");
            if (parts.length != 2) {
                return false;
            }
            int hours = Integer.parseInt(parts[0]);
            int minutes = Integer.parseInt(parts[1]);
            return hours >= 0 && hours <= 23 && minutes >= 0 && minutes <= 59;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    protected void execute() {
        logInfo("Starting arena task.");
        
        // Navigate to marksman camp
        if (!navigateToArena()) {
			logInfo("Failed to navigate to arena.");
			rescheduleWithActivationHour();
            return;
        }

        // Check for first run condition
        firstRun = checkFirstRun();

        // Click on Challenge button
        if (!openChallengeList()) {
			logInfo("Failed to open challenge list.");
            rescheduleWithActivationHour();
            return;
        }

		// Set initial attempts
		if (!getAttempts()) {
			logInfo("Failed to read initial attempts");
			rescheduleWithActivationHour();
			return;
		}

		// Buy extra attempts if configured
		if (extraAttempts > 0) {
			int attemptsBought = buyExtraAttempts();
			attempts += attemptsBought;
		}

        // Process challenges
        if (!processChallenges()) {
			logInfo("Failed to process challenges.");
            rescheduleWithActivationHour();
            return;
        }

        rescheduleWithActivationHour();
    }
	
	private boolean navigateToArena() {
		// Navigate to marksman camp first
        // This sequence of taps is intended to open the event list.
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
        sleepTask(500);
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(20, 250), new DTOPoint(200, 280));
        sleepTask(500);
		
        // Click on marksman camp shortcut
        DTOImageSearchResult marksmanResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_SHORTCUTS_MARKSMAN, 90);
        if (!marksmanResult.isFound()) {
			logWarning("Marksman camp shortcut not found.");
            return false;
        }
		emuManager.tapAtPoint(EMULATOR_NUMBER, marksmanResult.getPoint());
        sleepTask(1000);
		
        // Open arena
		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(702, 727));
        sleepTask(1000);
        return true;
    }

    private boolean checkFirstRun() {
        try {
			// Get arena score
            String arenaScoreText = emuManager.ocrRegionText(EMULATOR_NUMBER,
				new DTOPoint(567, 1065), new DTOPoint(649, 1099));
            long arenaScore = extractPowerValue(arenaScoreText);
            logInfo("Arena score: " + arenaScore);
            if(arenaScore == 1000) {
                logInfo("First run detected based on arena score of 1000.");
                return true;
            } else {
                return false;
            }
		} catch (Exception e) {
			logError("Failed to read arena score value: " + e.getMessage());
			return false;
		}
    }

    private boolean openChallengeList() {
		DTOImageSearchResult challengeResult = emuManager.searchTemplate(EMULATOR_NUMBER,EnumTemplates.ARENA_CHALLENGE_BUTTON, 90);
        if (!challengeResult.isFound()) {
			logWarning("Challenge button not found.");
            return false;
        }
		
        emuManager.tapAtPoint(EMULATOR_NUMBER, challengeResult.getPoint());
        sleepTask(1000);
        return true;
    }
	
    private boolean processChallenges() {
		long myPower;
		try {
			// Get my power first
            String myPowerText = emuManager.ocrRegionText(EMULATOR_NUMBER,
				MY_POWER_TOP_LEFT, MY_POWER_BOTTOM_RIGHT);
            myPower = extractPowerValue(myPowerText);
            logInfo("My power: " + myPower);
		} catch (Exception e) {
			logError("Failed to read my power value: " + e.getMessage());
			return false;
		}

		while (attempts > 0) {
            try {
                boolean foundOpponent = false;
                // Process each opponent from top to bottom
                for (int i = 0; i < 5; i++) {
                    // Calculate Y position for each opponent (starting from top)
                    int y;
                    if(firstRun) {
                        y = 369 + (i * 128);
                    } else {
                        y = 343 + (i * 128);
                    }
                    
                    // Read opponent power with multiple attempts
                    long bestPowerRead = 0;
                    logInfo("Reading power for opponent " + (i + 1) + " (position y=" + y + ")");
                    
                    for (int attempt = 0; attempt < 3; attempt++) {
                        logDebug("Attempt " + (attempt + 1) + "/3");
                        String opponentPowerText = emuManager.ocrRegionText(EMULATOR_NUMBER,
                                new DTOPoint(178, y), new DTOPoint(282, y + 34));
                        logDebug("Raw OCR text: '" + opponentPowerText + "'");
                        
                        long powerRead = extractPowerValue(opponentPowerText);
                        logDebug("Parsed power value: " + powerRead);
                        
                        // Validate the read value
                        boolean isValidPower = powerRead >= 50_000 && 
                                             String.valueOf(powerRead).length() >= 5 && 
                                             String.valueOf(powerRead).length() <= 9;
                                             
                        if (isValidPower) {
                            logDebug("Valid power reading: " + powerRead);
                            // If this is our first valid reading or it's higher than our previous best
                            // (assuming higher values are more likely to be correct due to OCR missing digits)
                            if (bestPowerRead == 0 || powerRead > bestPowerRead) {
                                logDebug("New best power reading (previous: " + bestPowerRead + ", new: " + powerRead + ")");
                                bestPowerRead = powerRead;
                            }
                        } else {
                            logDebug("Invalid power reading: " + powerRead + 
                                    " (must be >= 50,000 and have 5-9 digits)");
                        }
                        
                        if (attempt < 2) { // Don't sleep after the last attempt
                            sleepTask(300); // Short delay between attempts
                        }
                    }
                    
                    if (bestPowerRead == 0) {
                        logWarning("Failed to get valid power reading for opponent " + (i + 1) + 
                                 " after 3 attempts. Skipping this opponent.");
                        continue; // Skip this opponent
                    }
                    
                    logInfo("Final power reading for opponent " + (i + 1) + ": " + bestPowerRead + 
                           " (compared to my power: " + myPower + ")");

                    if (bestPowerRead < myPower) {
						// Click the challenge button for this opponent
                        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(624, y));
                        sleepTask(1000);
                        
                        // Handle the battle and continue with remaining attempts
                        handleBattle();
                        attempts--;
                        if(!checkResult()) {
                            continue; // If we lost, continue to next opponent
                        }
                        foundOpponent = true;
                        firstRun = false;
                        break;
                    }
                }
				
                if (!foundOpponent) {
					// Try to refresh the opponent list
                    if (!refreshOpponentList()) {
						logInfo("No more refreshes available and no suitable opponents found.");
                        return false;
                    }
                    sleepTask(1000);
                }
				
            } catch (Exception e) {
				logError("Failed to read power values: " + e.getMessage());
                return false;
            }
        }
		
        logInfo("All attempts used.");
        return true;
    }
	
    private long extractPowerValue(String powerText) {
        if (powerText == null || powerText.trim().isEmpty()) {
            logWarning("Empty power text received");
            return 0;
        }

        try {
            // Remove common OCR artifacts and clean the text
            String cleaned = powerText.toLowerCase()
                .replace(",", "")
                .replace(" ", "")
                .replace("m]", "m")
                // Common million value misreadings
                .replace("bom", "80m")
                .replace("bim", "81m")
                .replace("som", "50m")
                .replace("b7m", "87m")
                // Common letter/number confusions
                .replace("o", "0")  // Letter O to number 0
                .replace("l", "1")  // Lowercase L to number 1
                .replace("i", "1")  // Uppercase I to number 1
                .replace("z", "2")  // Letter Z to number 2
                .replace("s", "5")  // Letter S to number 5
                .replace("g", "6")  // Letter G to number 6
                .replace("b", "8")  // Letter B to number 8
                // Remove other common artifacts
                .replace("—", "")
                .replace("§", "")
                .trim();

            // If after cleaning we have nothing left, return 0
            if (cleaned.isEmpty()) {
                logWarning("Text cleaned to empty string: " + powerText);
                return 0;
            }

            // Handle million (M) values
            if (cleaned.endsWith("m")) {
                String numberPart = cleaned.substring(0, cleaned.length() - 1);
                // Handle decimal points in million values
                if (numberPart.contains(".")) {
                    double value = Double.parseDouble(numberPart);
                    return (long) (value * 1_000_000);
                } else {
                    long value = Long.parseLong(numberPart);
                    return value * 1_000_000;
                }
            }

            // Handle regular numbers
            return Long.parseLong(cleaned);
        } catch (NumberFormatException e) {
            logWarning("Failed to parse power value: " + powerText);
            return 0;
        }
    }
	
    private void handleBattle() {
		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(530, 1200)); // Tap to start battle
        sleepTask(3000);
        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(60, 962)); // Tap pause button
        sleepTask(500);
        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(252, 635)); // Tap retreat button to skip arena animation
        sleepTask(1000);
    }
	
    private boolean refreshOpponentList() {
		// Try free refresh first
        DTOImageSearchResult freeRefreshResult = emuManager.searchTemplate(
			EMULATOR_NUMBER, EnumTemplates.ARENA_FREE_REFRESH_BUTTON, 90);
			
        if (freeRefreshResult.isFound()) {
			logInfo("Using free refresh");
            emuManager.tapAtPoint(EMULATOR_NUMBER, freeRefreshResult.getPoint());
            return true;
        }

        // Check if refresh with gems is available and enabled
        if (refreshWithGems) {
			DTOImageSearchResult gemsRefreshResult = emuManager.searchTemplate(
                EMULATOR_NUMBER, EnumTemplates.ARENA_GEMS_REFRESH_BUTTON, 90);
				
            if (gemsRefreshResult.isFound()) {
				logInfo("Using gems refresh");
                emuManager.tapAtPoint(EMULATOR_NUMBER, gemsRefreshResult.getPoint());
                sleepTask(500);
				
                // Check for confirmation popup
                DTOImageSearchResult confirmResult = emuManager.searchTemplate(
					EMULATOR_NUMBER, EnumTemplates.ARENA_GEMS_REFRESH_CONFIRM_BUTTON, 90);
                
					if (confirmResult.isFound()) {
						emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(210,712));
						sleepTask(300);
                    emuManager.tapAtPoint(EMULATOR_NUMBER, confirmResult.getPoint());
                }
                return true;
            }
        }

        return false;
    }
	
    private boolean checkResult() {
        try {
            // Wait a bit longer for the result screen to stabilize
            sleepTask(1000);
            
            String result = emuManager.ocrRegionText(EMULATOR_NUMBER,
                new DTOPoint(165, 387), new DTOPoint(544, 495));
            
            // Clean up the result text
            String cleanResult = result != null ? result.toLowerCase()
                .replace("—", "")
                .replace("gs", "")
                .replace("fs", "")
                .replace("es", "")
                .replace("aa", "")
                .trim() : "";
                
            if (cleanResult.contains("victory")) {
                logInfo("Battle result: " + cleanResult);
                return true;
            } else if (cleanResult.contains("defeat")) {
                logInfo("Battle result: " + cleanResult);
            } else {
                // If we can't read the result, just log it and continue
                logWarning("Unrecognized battle result: " + result);
            }
            
            sleepTask(500); // Wait before tapping back
            tapBackButton();
        } catch (Exception e) {
            logError("OCR error while checking battle result: " + e.getMessage());
        }
        return false;
    }
	
	private boolean getAttempts() {
		try {
			String attemptsText = emuManager.ocrRegionText(EMULATOR_NUMBER,
			CHALLENGES_LEFT_TOP_LEFT, CHALLENGES_LEFT_BOTTOM_RIGHT);
			if (attemptsText != null) {
				attempts = Integer.parseInt(attemptsText.trim());
				logInfo("Initial attempts available: " + attempts);
				return true;
			}
			logWarning("Failed to parse attempts text: " + attemptsText);
			return false;
		} catch (Exception e) {
			logError("OCR error while reading attempts: " + e.getMessage());
			return false;
		}
	}

	private int buyExtraAttempts() {
		// Tap the "+" attempts button
		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(467, 965));
		sleepTask(1000);

		// Reset the queue counter to zero first
		logDebug("Resetting queue counter");
		emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(420, 733), new DTOPoint(40, 733));
		sleepTask(300);
		
		logInfo("Attempting to buy " + extraAttempts + " extra attempts");
		// Tap (extra attempts - 1) times to set the desired number of queues
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(457, 713), new DTOPoint(499, 752),
				(extraAttempts - 1), 400);
		sleepTask(300);

        Integer attemptsBought = readNumberValue(new DTOPoint(542, 714), new DTOPoint(619, 753));
        if(attemptsBought == null) {
            logWarning("Failed to read number of attempts bought, assuming " + extraAttempts);
            attemptsBought = extraAttempts;
        } else {
            logInfo("Confirmed attempts to be bought: " + attemptsBought);
        }

		emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(360, 860)); // Tap buy button

        return attemptsBought;
	}

	/**
	 * Schedules the task based on the configured activation time in UTC
	 */
	private void scheduleActivationTime() {
		try {
			// Parse the activation time
			String[] timeParts = activationHour.split(":");
			int hour = Integer.parseInt(timeParts[0]);
			int minute = Integer.parseInt(timeParts[1]);
			
			// Get the current UTC time
			ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
			
			// Create a UTC time for today at the activation time
			ZonedDateTime activationTimeUtc = nowUtc.toLocalDate().atTime(hour, minute).atZone(ZoneId.of("UTC"));
			
			// If the activation time has already passed today, schedule for tomorrow
			if (nowUtc.isAfter(activationTimeUtc)) {
				activationTimeUtc = activationTimeUtc.plusDays(1);
			}
			
			// Convert UTC time to system default time zone
			ZonedDateTime localActivationTime = activationTimeUtc.withZoneSameInstant(ZoneId.systemDefault());
			
			// Schedule the task
			logInfo("Scheduling Arena task for activation at " + activationHour + " UTC (" + 
					localActivationTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + " local time)");
			reschedule(localActivationTime.toLocalDateTime());
		} catch (Exception e) {
			logError("Failed to schedule activation time: " + e.getMessage());
		}
	}

	/**
	 * Special reschedule method that respects the configured activation time
	 * If activation time is configured in valid HH:mm format, it uses that time for the next day
	 * Otherwise, it uses the standard game reset time
	 */
	private void rescheduleWithActivationHour() {
		// If activation hour is configured and valid
		if (isValidTimeFormat(activationHour)) {
			try {
				// Parse the activation time
				String[] timeParts = activationHour.split(":");
				int hour = Integer.parseInt(timeParts[0]);
				int minute = Integer.parseInt(timeParts[1]);
				
				// Schedule based on the configured activation time for the next day
				ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
				ZonedDateTime tomorrowActivationUtc = nowUtc.toLocalDate().plusDays(1)
					.atTime(hour, minute).atZone(ZoneId.of("UTC"));
				ZonedDateTime localActivationTime = tomorrowActivationUtc.withZoneSameInstant(ZoneId.systemDefault());
				
				logInfo("Rescheduling Arena task for next activation at " + activationHour + 
					" UTC tomorrow (" + localActivationTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + 
					" local time)");
				
				reschedule(localActivationTime.toLocalDateTime());
				return;
			} catch (Exception e) {
				logError("Failed to reschedule with activation time: " + e.getMessage());
			}
		}
		
		// Use standard game reset time if activation time is invalid or an error occurred
		logInfo("Rescheduling Arena task for game reset time");
		reschedule(UtilTime.getGameReset());
	}

	@Override
	public EnumStartLocation getRequiredStartLocation() {
		return EnumStartLocation.HOME;
	}

    @Override
    public boolean provideDailyMissionProgress() {
        return true;
    }
}