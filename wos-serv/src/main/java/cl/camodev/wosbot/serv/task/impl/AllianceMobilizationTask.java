package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Task responsible for Alliance Mobilization.
 * It will use OCR and image recognition to select and perform tasks.
 */
public class AllianceMobilizationTask extends DelayedTask {

    public AllianceMobilizationTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
        // TODO: Add scheduling logic if needed, similar to ArenaTask
    }

    @Override
    protected void execute() {
        logInfo("╔═══════════════════════════════════════════════════════════════╗");
        logInfo("║  Starting Alliance Mobilization Task                         ║");
        logInfo("║  Profile: " + String.format("%-48s", profile.getName()) + "║");
        logInfo("╚═══════════════════════════════════════════════════════════════╝");

        // Print current configuration
        printConfiguration();

        // Check if Alliance Mobilization is enabled
        boolean allianceMobilizationEnabled = profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_BOOL, Boolean.class);
        if (!allianceMobilizationEnabled) {
            logInfo("Alliance Mobilization is disabled for this profile. Skipping task.");
            this.setRecurring(false);
            return;
        }

        // 1. Navigate to the Alliance Mobilization screen.
        logInfo("\n[STEP 1/2] Navigating to Alliance Mobilization...");
        if (!navigateToAllianceMobilization()) {
            logInfo("❌ Failed to navigate to Alliance Mobilization.");
            // TODO: Add rescheduling logic
            return;
        }
        logInfo("✅ Navigation successful");

        // 2. Analyze the available tasks.
        logInfo("\n[STEP 2/2] Analyzing and performing tasks...");
        analyzeAndPerformTasks();

        logInfo("\n╔═══════════════════════════════════════════════════════════════╗");
        logInfo("║  Alliance Mobilization Task Finished                         ║");
        logInfo("╚═══════════════════════════════════════════════════════════════╝");
        // TODO: Add rescheduling logic
    }

    private void printConfiguration() {
        logInfo("\n📋 Current Configuration:");
        logInfo("  • Rewards Filter: " + profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_REWARDS_PERCENTAGE_STRING, String.class));
        logInfo("  • Minimum Points: " + profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_MINIMUM_POINTS_INT, Integer.class));
        logInfo("  • Train Troops: " + (profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_TRAIN_TROOPS_BOOL, Boolean.class) ? "✅" : "❌"));
        logInfo("  • Build Speedups: " + (profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_BUILD_SPEEDUPS_BOOL, Boolean.class) ? "✅" : "❌"));
        logInfo("  • Chief Gear Score: " + (profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_CHIEF_GEAR_SCORE_BOOL, Boolean.class) ? "✅" : "❌"));
        logInfo("  • Debug Mode: ✅ (FORCED ON FOR TESTING)");
        logInfo("");
    }

    private boolean navigateToAllianceMobilization() {
        logInfo("Navigating to Alliance Mobilization...");

        // Step 1: Click the Events button to open the events screen
        logDebug("Searching for Events button on home screen...");
        DTOImageSearchResult eventsButton = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.HOME_EVENTS_BUTTON, 90);
        if (!eventsButton.isFound()) {
            logWarning("Events button not found on home screen.");
            captureDebugScreenshot("events_button_not_found");
            return false;
        }
        logDebug("Events button found at: " + eventsButton.getPoint());
        emuManager.tapAtPoint(EMULATOR_NUMBER, eventsButton.getPoint());
        sleepTask(2000); // Increased wait time for events screen to load

        // Step 2: Try to find the event via tabs, similar to JourneyOfLight
        logDebug("Searching for Alliance Mobilization tabs...");
        DTOImageSearchResult selectedTab = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_MOBILIZATION_TAB, 90);
        DTOImageSearchResult unselectedTab = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_MOBILIZATION_UNSELECTED_TAB, 90);

        logDebug("Selected tab found: " + selectedTab.isFound());
        logDebug("Unselected tab found: " + unselectedTab.isFound());

        if (selectedTab.isFound()) {
            logInfo("Alliance Mobilization tab is already selected at: " + selectedTab.getPoint());
            captureDebugScreenshot("tab_already_selected");
        } else if (unselectedTab.isFound()) {
            logInfo("Found unselected Alliance Mobilization tab at: " + unselectedTab.getPoint() + ", clicking it.");
            emuManager.tapAtPoint(EMULATOR_NUMBER, unselectedTab.getPoint());
            sleepTask(2000);
            captureDebugScreenshot("after_tab_click");
        } else {
            logInfo("Alliance Mobilization tabs not found, swiping to search for them...");
            captureDebugScreenshot("before_tab_search");

            // Search for tabs by swiping left to right (shows tabs to the right)
            boolean tabFound = false;
            for (int i = 0; i < 2; i++) {
                logDebug("Tab search attempt " + (i + 1) + "/2");

                // Check for tabs after each swipe
                DTOImageSearchResult selectedTabAfterSwipe = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_MOBILIZATION_TAB, 90);
                DTOImageSearchResult unselectedTabAfterSwipe = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_MOBILIZATION_UNSELECTED_TAB, 90);

                if (selectedTabAfterSwipe.isFound()) {
                    logInfo("Found selected Alliance Mobilization tab after swipe " + (i + 1) + " at: " + selectedTabAfterSwipe.getPoint());
                    tabFound = true;
                    break;
                } else if (unselectedTabAfterSwipe.isFound()) {
                    logInfo("Found unselected Alliance Mobilization tab after swipe " + (i + 1) + " at: " + unselectedTabAfterSwipe.getPoint());
                    emuManager.tapAtPoint(EMULATOR_NUMBER, unselectedTabAfterSwipe.getPoint());
                    sleepTask(2000);
                    tabFound = true;
                    break;
                }

                // Swipe left to right to see more tabs (centered for 720x1280)
                if (i < 4) {
                    logDebug("Swiping left to right...");
                    emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(100, 158), new DTOPoint(620, 158));
                    sleepTask(500);
                }
            }

            if (!tabFound) {
                logWarning("Alliance Mobilization tabs not found after multiple swipes. Event may not be active.");
                captureDebugScreenshot("tab_search_failed");
                return false;
            }
        }

        logInfo("Successfully navigated to Alliance Mobilization.");
        captureDebugScreenshot("navigation_success");
        return true;
    }

    private void analyzeAndPerformTasks() {
        logInfo("Analyzing and performing Alliance Mobilization tasks...");
        captureDebugScreenshot("before_task_analysis");

        // Get user configuration for reward percentage filter
        String rewardsPercentage = profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_REWARDS_PERCENTAGE_STRING, String.class);
        int minimumPoints = profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_MINIMUM_POINTS_INT, Integer.class);

        logInfo("Filtering tasks by: " + rewardsPercentage + " with minimum points: " + minimumPoints);

        // Search and process tasks based on bonus percentage
        searchAndProcessTasksByBonus(rewardsPercentage, minimumPoints);
    }

    private void searchAndProcessTasksByBonus(String rewardsPercentage, int minimumPoints) {
        // First, check for completed tasks to collect rewards
        checkAndCollectCompletedTasks();

        logInfo("=== Searching for tasks by bonus percentage ===");

        // Determine which templates to search for based on user selection
        boolean search200 = rewardsPercentage.equals("200%") || rewardsPercentage.equals("Both") || rewardsPercentage.equals("Any");
        boolean search120 = rewardsPercentage.equals("120%") || rewardsPercentage.equals("Both") || rewardsPercentage.equals("Any");

        // Search for 200% bonus (only 1 can exist)
        if (search200) {
            logDebug("Searching for 200% bonus task...");
            DTOImageSearchResult result200 = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.AM_200_PERCENT, 85);
            debugTemplateSearch("AM_200_PERCENT", result200, 85);

            if (result200.isFound()) {
                captureDebugScreenshot("found_200_percent");

                // Check if task is already running (orange bar with timer below bonus)
                if (isTaskAlreadyRunning(result200.getPoint())) {
                    logInfo("Task at 200% is already running (timer bar detected) - skipping");
                } else {
                    // Check task type near the bonus indicator
                    EnumTemplates taskType = detectTaskTypeNearBonus(result200.getPoint());
                    if (taskType != null) {
                        logInfo("Task type detected: " + taskType.name());
                        boolean isEnabled = isTaskTypeEnabled(taskType);

                        // Process the task and exit
                        processTask(result200.getPoint(), taskType, isEnabled, minimumPoints);
                        return; // Exit after processing one task
                    } else {
                        logInfo("Task type not detected");
                    }
                }
            }
        }

        // Search for 120% bonus tasks (up to 2 can exist)
        if (search120) {
            logDebug("Searching for 120% bonus tasks (max 2 positions)...");
            List<DTOImageSearchResult> results120 = emuManager.searchTemplates(EMULATOR_NUMBER, EnumTemplates.AM_120_PERCENT, 85, 2);

            if (results120 != null && !results120.isEmpty()) {
                logInfo("Found " + results120.size() + " x 120% bonus task(s)");

                for (DTOImageSearchResult result120 : results120) {
                    debugTemplateSearch("AM_120_PERCENT", result120, 85);
                    captureDebugScreenshot("found_120_percent");

                    // Check if task is already running (orange bar with timer below bonus)
                    if (isTaskAlreadyRunning(result120.getPoint())) {
                        logInfo("Task at 120% (" + result120.getPoint() + ") is already running - skipping");
                        continue; // Check next position
                    }

                    // Check task type near the bonus indicator
                    EnumTemplates taskType = detectTaskTypeNearBonus(result120.getPoint());
                    if (taskType != null) {
                        logInfo("Task type detected: " + taskType.name());
                        boolean isEnabled = isTaskTypeEnabled(taskType);

                        // Process the task and exit
                        processTask(result120.getPoint(), taskType, isEnabled, minimumPoints);
                        return; // Exit after processing one task
                    } else {
                        logInfo("Task type not detected at " + result120.getPoint());
                    }
                }
            } else {
                logDebug("No 120% bonus tasks found");
            }
        }
    }

    private EnumTemplates detectTaskTypeNearBonus(DTOPoint bonusLocation) {
        logDebug("Detecting task type near bonus location: " + bonusLocation);

        // Define task type templates to search for
        EnumTemplates[] taskTypeTemplates = {
            EnumTemplates.AM_BUILD_SPEEDUPS,
            EnumTemplates.AM_BUY_PACKAGE,
            EnumTemplates.AM_CHIEF_GEAR_CHARM,
            EnumTemplates.AM_CHIEF_GEAR_SCORE,
            EnumTemplates.AM_DEFEAT_BEASTS,
            EnumTemplates.AM_FIRE_CRYSTAL,
            EnumTemplates.AM_GATHER_RESOURCES,
            EnumTemplates.AM_HERO_GEAR_STONE,
            EnumTemplates.AM_MYTHIC_SHARD,
            EnumTemplates.AM_RALLY,
            EnumTemplates.AM_TRAIN_TROOPS,
            EnumTemplates.AM_TRAINING_SPEEDUPS,
            EnumTemplates.AM_USE_GEMS,
            EnumTemplates.AM_USE_SPEEDUPS
        };

        // Search for task type icon near the bonus indicator (typically to the left)
        for (EnumTemplates template : taskTypeTemplates) {
            DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, template, 85);
            if (result.isFound()) {
                // Verify the task type icon is near the bonus indicator
                int deltaX = Math.abs(result.getPoint().getX() - bonusLocation.getX());
                int deltaY = Math.abs(result.getPoint().getY() - bonusLocation.getY());

                logDebug("  " + template.name() + " found at (" + result.getPoint().getX() + "," +
                        result.getPoint().getY() + ") - Distance from bonus: ΔX=" + deltaX + "px, ΔY=" + deltaY + "px");

                // Task icon should be within reasonable distance (adjust based on UI layout)
                if (deltaX < 150 && deltaY < 100) {
                    logInfo("✅ Detected task type: " + template.name() + " at " + result.getPoint() +
                           " (ΔX=" + deltaX + "px, ΔY=" + deltaY + "px)");
                    return template;
                } else {
                    logDebug("  ❌ Too far from bonus (max: ΔX=150px, ΔY=100px)");
                }
            }
        }

        logWarning("No task type detected near bonus location");
        return null;
    }

    private boolean isTaskAlreadyRunning(DTOPoint bonusLocation) {
        logDebug("Checking if task is already running near: " + bonusLocation);

        // Search for AM_Bar.png (orange timer bar) below the bonus indicator
        // The bar appears approximately 150-200px below the bonus icon
        DTOPoint searchTopLeft = new DTOPoint(bonusLocation.getX() - 50, bonusLocation.getY() + 100);
        DTOPoint searchBottomRight = new DTOPoint(bonusLocation.getX() + 250, bonusLocation.getY() + 250);

        DTOImageSearchResult barResult = emuManager.searchTemplate(
            EMULATOR_NUMBER,
            EnumTemplates.AM_BAR,
            searchTopLeft,
            searchBottomRight,
            85
        );

        if (barResult.isFound()) {
            logInfo("✅ Timer bar detected at " + barResult.getPoint() + " - task is already running");
            return true;
        }

        logDebug("No timer bar detected - task is available");
        return false;
    }

    private boolean isTaskTypeEnabled(EnumTemplates taskType) {
        logDebug("Checking if task type is enabled: " + taskType.name());

        // Map template to configuration key
        switch (taskType) {
            case AM_BUILD_SPEEDUPS:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_BUILD_SPEEDUPS_BOOL, Boolean.class);
            case AM_BUY_PACKAGE:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_BUY_PACKAGE_BOOL, Boolean.class);
            case AM_CHIEF_GEAR_CHARM:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_CHIEF_GEAR_CHARM_BOOL, Boolean.class);
            case AM_CHIEF_GEAR_SCORE:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_CHIEF_GEAR_SCORE_BOOL, Boolean.class);
            case AM_DEFEAT_BEASTS:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_DEFEAT_BEASTS_BOOL, Boolean.class);
            case AM_FIRE_CRYSTAL:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_FIRE_CRYSTAL_BOOL, Boolean.class);
            case AM_GATHER_RESOURCES:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_GATHER_RESOURCES_BOOL, Boolean.class);
            case AM_HERO_GEAR_STONE:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_HERO_GEAR_STONE_BOOL, Boolean.class);
            case AM_MYTHIC_SHARD:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_MYTHIC_SHARD_BOOL, Boolean.class);
            case AM_RALLY:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_RALLY_BOOL, Boolean.class);
            case AM_TRAIN_TROOPS:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_TRAIN_TROOPS_BOOL, Boolean.class);
            case AM_TRAINING_SPEEDUPS:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_TRAINING_SPEEDUPS_BOOL, Boolean.class);
            case AM_USE_GEMS:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_USE_GEMS_BOOL, Boolean.class);
            case AM_USE_SPEEDUPS:
                return profile.getConfig(EnumConfigurationKey.ALLIANCE_MOBILIZATION_USE_SPEEDUPS_BOOL, Boolean.class);
            default:
                logWarning("Unknown task type: " + taskType.name());
                return false;
        }
    }

    private void processTask(DTOPoint bonusLocation, EnumTemplates taskType, boolean isTaskTypeEnabled, int minimumPoints) {
        logInfo("Processing task: " + taskType.name() + " at " + bonusLocation + " (Enabled: " + isTaskTypeEnabled + ")");
        captureDebugScreenshot("before_task_click");

        // Click on the task to open the selection screen
        emuManager.tapAtPoint(EMULATOR_NUMBER, bonusLocation);
        sleepTask(2000);

        captureDebugScreenshot("after_task_click");

        // Check the selection screen and decide whether to Accept or Refresh
        handleTaskSelectionScreen(taskType, isTaskTypeEnabled, minimumPoints);
    }

    private void handleTaskSelectionScreen(EnumTemplates taskType, boolean isTaskTypeEnabled, int minimumPoints) {
        logInfo("=== Handling Task Selection Screen ===");
        logInfo("Task type: " + taskType.name() + ", Enabled: " + isTaskTypeEnabled + ", Minimum points: " + minimumPoints);
        captureDebugScreenshot("task_selection_screen");

        // Fixed button coordinates
        DTOPoint acceptButtonLocation = new DTOPoint(500, 805);
        DTOPoint refreshButtonLocation = new DTOPoint(200, 805);

        // Decision logic:
        // 1. If task type is NOT enabled -> always Refresh
        // 2. If task type IS enabled -> check points on selection screen

        if (!isTaskTypeEnabled) {
            logInfo("Task type NOT enabled in UI -> Refreshing mission");
            emuManager.tapAtPoint(EMULATOR_NUMBER, refreshButtonLocation);
            sleepTask(1500);
            captureDebugScreenshot("after_refresh_click");

            // Read cooldown timer from confirmation popup - focus on "5mins!" part
            DTOPoint timerTopLeft = new DTOPoint(375, 610);
            DTOPoint timerBottomRight = new DTOPoint(490, 642);
            int cooldownSeconds = readRefreshCooldownFromPopup(timerTopLeft, timerBottomRight);

            // Click Refresh button (blue button on the right)
            DTOPoint refreshConfirmButtonLocation = new DTOPoint(510, 790);
            logInfo("Confirming refresh at: " + refreshConfirmButtonLocation);
            emuManager.tapAtPoint(EMULATOR_NUMBER, refreshConfirmButtonLocation);
            sleepTask(1500);
            captureDebugScreenshot("after_refresh_confirmed");

            // Reschedule task for after cooldown expires
            if (cooldownSeconds > 0) {
                LocalDateTime nextRun = LocalDateTime.now().plusSeconds(cooldownSeconds + 5);
                this.reschedule(nextRun);
                logInfo("Mission refreshed (task type not enabled). Rescheduled for " + nextRun);
            } else {
                logInfo("Mission refreshed (task type not enabled). No cooldown detected.");
            }
            return;
        }

        // Task type IS enabled -> verify points on selection screen
        logInfo("Task type IS enabled -> Checking points on selection screen");

        // Read the points value from the selection screen
        int detectedPoints = readPointsFromSelectionScreen();

        if (detectedPoints < 0) {
            logWarning("Could not read points from selection screen, refreshing as fallback");
            emuManager.tapAtPoint(EMULATOR_NUMBER, refreshButtonLocation);
            sleepTask(1500);
            captureDebugScreenshot("after_refresh_ocr_failed");
            return;
        }

        logInfo("Detected points on selection screen: " + detectedPoints);

        // Decision: Accept if points >= minimum, otherwise Refresh
        if (detectedPoints >= minimumPoints) {
            logInfo("Points meet minimum (" + detectedPoints + " >= " + minimumPoints + ") -> Accepting task");
            emuManager.tapAtPoint(EMULATOR_NUMBER, acceptButtonLocation);
            sleepTask(1500);
            captureDebugScreenshot("after_accept");
            logInfo("✅ Task accepted successfully");
        } else {
            logInfo("Points below minimum (" + detectedPoints + " < " + minimumPoints + ") -> Refreshing mission");
            emuManager.tapAtPoint(EMULATOR_NUMBER, refreshButtonLocation);
            sleepTask(1500);
            captureDebugScreenshot("after_refresh_click");

            // Read cooldown timer from confirmation popup - focus on "5mins!" part
            DTOPoint timerTopLeft = new DTOPoint(375, 610);
            DTOPoint timerBottomRight = new DTOPoint(490, 642);
            int cooldownSeconds = readRefreshCooldownFromPopup(timerTopLeft, timerBottomRight);

            // Click Refresh button (blue button on the right)
            DTOPoint refreshConfirmButtonLocation = new DTOPoint(510, 790);
            logInfo("Confirming refresh at: " + refreshConfirmButtonLocation);
            emuManager.tapAtPoint(EMULATOR_NUMBER, refreshConfirmButtonLocation);
            sleepTask(1500);
            captureDebugScreenshot("after_refresh_confirmed");

            // Reschedule task for after cooldown expires
            if (cooldownSeconds > 0) {
                LocalDateTime nextRun = LocalDateTime.now().plusSeconds(cooldownSeconds + 5);
                this.reschedule(nextRun);
                logInfo("Mission refreshed (points too low). Rescheduled for " + nextRun);
            } else {
                logInfo("Mission refreshed (points too low). No cooldown detected.");
            }
        }
    }

    private int readRefreshCooldownFromPopup(DTOPoint topLeft, DTOPoint bottomRight) {
        logDebug("Reading refresh cooldown from popup: " + topLeft + " to " + bottomRight);

        // Wait a bit for popup to fully render
        sleepTask(500);

        try {
            String ocrResult = emuManager.ocrRegionText(EMULATOR_NUMBER, topLeft, bottomRight);
            logInfo("OCR popup result: '" + (ocrResult != null ? ocrResult : "null") + "'");
            debugOCRArea("Cooldown popup", topLeft, bottomRight, ocrResult);

            if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                // Parse time format: "5mins!", "10mins", "1min", etc.
                int seconds = parseTimeToSeconds(ocrResult);
                if (seconds > 0) {
                    logInfo("✅ Cooldown from popup: " + seconds + " seconds (from: '" + ocrResult + "')");
                    return seconds;
                } else {
                    logWarning("OCR returned text but could not parse time: '" + ocrResult + "'");
                }
            } else {
                logWarning("OCR returned empty or null result");
            }
        } catch (Exception e) {
            logWarning("Failed to read cooldown from popup: " + e.getMessage());
            e.printStackTrace();
        }

        logWarning("Could not read cooldown from confirmation popup, using default 5 minutes");
        return 300; // Default 5 minutes if OCR fails
    }

    private int readRefreshCooldownTimer(DTOPoint refreshButtonLocation) {
        logDebug("Reading refresh cooldown timer near button: " + refreshButtonLocation);

        // The cooldown timer is typically displayed near the Refresh button
        // Try multiple OCR areas around the button
        DTOPoint[] ocrOffsets = {
            // Above the button
            new DTOPoint(0, -40),
            // Below the button
            new DTOPoint(0, 40),
            // To the right of the button
            new DTOPoint(80, 0),
            // To the left of the button
            new DTOPoint(-80, 0)
        };

        for (DTOPoint offset : ocrOffsets) {
            int testX = refreshButtonLocation.getX() + offset.getX();
            int testY = refreshButtonLocation.getY() + offset.getY();

            DTOPoint topLeft = new DTOPoint(testX - 50, testY - 15);
            DTOPoint bottomRight = new DTOPoint(testX + 50, testY + 15);

            try {
                String ocrResult = emuManager.ocrRegionText(EMULATOR_NUMBER, topLeft, bottomRight);
                logDebug("OCR cooldown result at offset " + offset + ": '" + ocrResult + "'");

                if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                    // Parse time format: could be "5:30", "05:30", "5m 30s", "5m30s", "330s", etc.
                    int seconds = parseTimeToSeconds(ocrResult);
                    if (seconds > 0) {
                        logInfo("Detected refresh cooldown: " + seconds + " seconds (from: '" + ocrResult + "')");
                        return seconds;
                    }
                }
            } catch (Exception e) {
                logDebug("OCR cooldown failed at offset " + offset + ": " + e.getMessage());
            }
        }

        logWarning("Could not read refresh cooldown timer");
        return 0;
    }

    private int parseTimeToSeconds(String timeString) {
        // Remove all spaces and normalize common variations
        String cleaned = timeString.replaceAll("\\s+", "").toLowerCase();

        // Remove common prefix words
        cleaned = cleaned.replaceAll("^(in|r|ear)", "");

        // Fix common OCR errors: "Smins" -> "5mins", "Zmins" -> "2mins"
        cleaned = cleaned.replaceAll("smins", "5mins");
        cleaned = cleaned.replaceAll("zmins", "2mins");

        // Normalize: "mins" -> "m", "min" -> "m", "secs" -> "s", "sec" -> "s"
        cleaned = cleaned.replaceAll("mins?", "m");
        cleaned = cleaned.replaceAll("secs?", "s");
        cleaned = cleaned.replaceAll("hours?", "h");
        // Remove exclamation marks and other punctuation
        cleaned = cleaned.replaceAll("[!.,]", "");

        try {
            // Primary Format: "HH:mm:ss" or "H:mm:ss" (hours:minutes:seconds)
            if (cleaned.matches("\\d{1,2}:\\d{2}:\\d{2}")) {
                String[] parts = cleaned.split(":");
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                int seconds = Integer.parseInt(parts[2]);
                return hours * 3600 + minutes * 60 + seconds;
            }

            // Fallback Format: "mm:ss" or "m:ss" (minutes:seconds)
            if (cleaned.matches("\\d{1,2}:\\d{2}")) {
                String[] parts = cleaned.split(":");
                int minutes = Integer.parseInt(parts[0]);
                int seconds = Integer.parseInt(parts[1]);
                return minutes * 60 + seconds;
            }

            // Format: "5m30s" or "5m 30s"
            if (cleaned.contains("m") && cleaned.contains("s")) {
                String minutesStr = cleaned.substring(0, cleaned.indexOf("m"));
                String secondsStr = cleaned.substring(cleaned.indexOf("m") + 1, cleaned.indexOf("s"));
                int minutes = Integer.parseInt(minutesStr);
                int seconds = Integer.parseInt(secondsStr);
                return minutes * 60 + seconds;
            }

            // Format: "5m" (only minutes)
            if (cleaned.matches("\\d+m")) {
                String minutesStr = cleaned.replace("m", "");
                int minutes = Integer.parseInt(minutesStr);
                return minutes * 60;
            }

            // Format: "330s" or "330" (only seconds)
            if (cleaned.matches("\\d+s?")) {
                String secondsStr = cleaned.replace("s", "");
                return Integer.parseInt(secondsStr);
            }

            // Format: "5h" (hours - convert to seconds)
            if (cleaned.matches("\\d+h")) {
                String hoursStr = cleaned.replace("h", "");
                int hours = Integer.parseInt(hoursStr);
                return hours * 3600;
            }

            // Format: "5h30m" (hours and minutes)
            if (cleaned.contains("h") && cleaned.contains("m")) {
                String hoursStr = cleaned.substring(0, cleaned.indexOf("h"));
                String minutesStr = cleaned.substring(cleaned.indexOf("h") + 1, cleaned.indexOf("m"));
                int hours = Integer.parseInt(hoursStr);
                int minutes = Integer.parseInt(minutesStr);
                return hours * 3600 + minutes * 60;
            }

        } catch (Exception e) {
            logDebug("Failed to parse time string '" + timeString + "': " + e.getMessage());
        }

        return 0;
    }

    private int readPointsFromSelectionScreen() {
        logDebug("Reading points from selection screen");

        // OCR area for points on selection screen: wider area to capture centered text ("+90" to "+1200")
        DTOPoint topLeft = new DTOPoint(320, 735);
        DTOPoint bottomRight = new DTOPoint(450, 765);

        logDebug("OCR area: " + topLeft + " to " + bottomRight);

        // Retry up to 3 times to read the points
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String ocrResult = emuManager.ocrRegionText(EMULATOR_NUMBER, topLeft, bottomRight);
                debugOCRArea("Selection screen points (attempt " + attempt + ")", topLeft, bottomRight, ocrResult);

                if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                    // Extract numeric value from OCR result
                    // Look for pattern "+XXX" to avoid capturing other numbers like "2) +860"
                    String numericValue = ocrResult.replaceAll(".*\\+\\s*", ""); // Remove everything before "+"
                    numericValue = numericValue.replaceAll("[^0-9]", ""); // Keep only digits

                    if (!numericValue.isEmpty()) {
                        int points = Integer.parseInt(numericValue);
                        logInfo("✅ Read points from selection screen: " + points + " (attempt " + attempt + ")");
                        return points;
                    } else {
                        logWarning("OCR result contains no numeric value: '" + ocrResult + "' (attempt " + attempt + ")");
                    }
                } else {
                    logWarning("OCR result is empty (attempt " + attempt + ")");
                }
            } catch (Exception e) {
                logWarning("OCR failed (attempt " + attempt + "): " + e.getMessage());
            }

            // Wait 500ms before retry (except after last attempt)
            if (attempt < maxRetries) {
                sleepTask(500);
            }
        }

        logWarning("Could not read points from selection screen after " + maxRetries + " attempts");
        return -1;
    }

    private void closeSelectionScreen() {
        logDebug("Attempting to close selection screen");
        // Press back or close button to exit the selection screen
        // This will need to be implemented based on the UI
        // For now, we'll just log and continue
        logInfo("Selection screen closed (or attempted to close)");
        sleepTask(1000);
    }

    private boolean verifyPointsNearBonus(DTOPoint bonusLocation, int minimumPoints) {
        logDebug("Verifying points near bonus location: " + bonusLocation);

        // The points are displayed at a specific offset from the bonus indicator
        // Based on testing: 120% symbol at (83,632) -> points at (195,790) to (270,824)
        // Offset: X+112px, Y+158px, Width: 75px, Height: 34px

        int offsetX = 112;
        int offsetY = 158;
        int width = 75;
        int height = 34;

        DTOPoint topLeft = new DTOPoint(bonusLocation.getX() + offsetX, bonusLocation.getY() + offsetY);
        DTOPoint bottomRight = new DTOPoint(topLeft.getX() + width, topLeft.getY() + height);

        logDebug("OCR area for points: " + topLeft + " to " + bottomRight);

        try {
            String ocrResult = emuManager.ocrRegionText(EMULATOR_NUMBER, topLeft, bottomRight);
            debugOCRArea("Points near bonus", topLeft, bottomRight, ocrResult);

            if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                // Extract numeric value from OCR result
                String numericValue = ocrResult.replaceAll("[^0-9]", "");

                if (!numericValue.isEmpty()) {
                    int points = Integer.parseInt(numericValue);
                    logInfo("✅ Detected points: " + points + " - Minimum required: " + minimumPoints);
                    return points >= minimumPoints;
                } else {
                    logWarning("OCR result contains no numeric value: '" + ocrResult + "'");
                }
            } else {
                logWarning("OCR result is empty");
            }
        } catch (Exception e) {
            logWarning("Failed to verify points via OCR: " + e.getMessage());
        }

        return false;
    }

    private void testTemplateDetection() {
        logInfo("=== Testing Template Detection ===");

        // Test all available templates to see what's currently visible
        String[] testTemplates = {
            "MOBILIZATION_EXCLUSIVE_BUTTON",
            // Add more template tests based on what we have
        };

        for (String templateName : testTemplates) {
            try {
                EnumTemplates template = EnumTemplates.valueOf(templateName);
                DTOImageSearchResult result = emuManager.searchTemplate(EMULATOR_NUMBER, template, 85);
                logDebug("Template " + templateName + ": " + (result.isFound() ? "FOUND at " + result.getPoint() : "NOT FOUND"));
            } catch (Exception e) {
                logDebug("Template " + templateName + ": ERROR - " + e.getMessage());
            }
        }
    }

    private void analyzeTaskCards() {
        logInfo("=== Analyzing Task Cards ===");

        // Look for task frames/cards
        try {
            DTOImageSearchResult taskFrame = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.valueOf("AM_FRAME"), 80);
            if (taskFrame.isFound()) {
                logInfo("Found task frame at: " + taskFrame.getPoint());
                captureDebugScreenshot("task_frame_found");

                // Test OCR in the area around the task frame
                testOCRAroundPoint(taskFrame.getPoint());
            } else {
                logInfo("No task frame found, searching for individual task elements...");
                // Try to find specific task types
                searchForTaskTypes();
            }
        } catch (IllegalArgumentException e) {
            logDebug("AM_FRAME template not found in enum, searching for individual elements...");
            searchForTaskTypes();
        }
    }

    private void searchForTaskTypes() {
        logInfo("=== Searching for Task Types ===");

        // Based on available templates, search for task-specific elements
        String[] taskTemplates = {
            "AM_Build_Speedups", "AM_Buy_Package", "AM_Chief_Gear_Charm",
            "AM_Defeat_Beasts", "AM_Fire_Crystal", "AM_Gather_Resources",
            "AM_Train_Troops", "AM_Use_Gems"
        };

        for (String templateName : taskTemplates) {
            try {
                // Try to find template (need to convert to proper enum)
                logDebug("Searching for task type: " + templateName);
                // This is a placeholder - we'd need to map these to actual enum values
            } catch (Exception e) {
                logDebug("Error searching for " + templateName + ": " + e.getMessage());
            }
        }
    }

    private void testBasicOCR() {
        logInfo("=== Testing Basic OCR ===");

        // Define test areas for OCR
        DTOPoint[] testAreas = {
            // Top area for percentage indicators
            new DTOPoint(100, 100),
            new DTOPoint(300, 150),
            // Middle area for task descriptions
            new DTOPoint(200, 250),
            new DTOPoint(400, 300),
            // Bottom area for rewards/costs
            new DTOPoint(150, 400),
            new DTOPoint(350, 450)
        };

        for (int i = 0; i < testAreas.length; i++) {
            DTOPoint testPoint = testAreas[i];
            logDebug("Testing OCR at region " + (i + 1) + " around point: " + testPoint);

            try {
                // Test OCR in a small area around each point
                testOCRAroundPoint(testPoint);
            } catch (Exception e) {
                logDebug("OCR test " + (i + 1) + " failed: " + e.getMessage());
            }
        }
    }

    private void testOCRAroundPoint(DTOPoint centerPoint) {
        // Define OCR area around the center point
        DTOPoint topLeft = new DTOPoint(centerPoint.getX() - 50, centerPoint.getY() - 25);
        DTOPoint bottomRight = new DTOPoint(centerPoint.getX() + 50, centerPoint.getY() + 25);

        logDebug("OCR test area: " + topLeft + " to " + bottomRight);

        try {
            String ocrResult = emuManager.ocrRegionText(EMULATOR_NUMBER, topLeft, bottomRight);
            if (ocrResult != null && !ocrResult.trim().isEmpty()) {
                logInfo("OCR detected text: '" + ocrResult.trim() + "' at " + centerPoint);
            } else {
                logDebug("No text detected in OCR area around " + centerPoint);
            }
        } catch (Exception e) {
            logDebug("OCR failed at " + centerPoint + ": " + e.getMessage());
        }
    }

    private void checkAndCollectCompletedTasks() {
        logDebug("Checking for completed tasks to collect rewards...");

        // Search for AM_Completed.png indicator
        DTOImageSearchResult completedResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.AM_COMPLETED, 85);

        if (completedResult.isFound()) {
            logInfo("✅ Completed task found at " + completedResult.getPoint() + " - collecting rewards");
            captureDebugScreenshot("completed_task_found");

            // Click on the completed task
            emuManager.tapAtPoint(EMULATOR_NUMBER, completedResult.getPoint());
            sleepTask(1500);
            captureDebugScreenshot("after_completed_task_click");

            logInfo("Rewards collected from completed task");
        } else {
            logDebug("No completed tasks found");
        }
    }

    // Helper method to capture debug screenshots
    private void captureDebugScreenshot(String description) {
        // Screenshots temporarily disabled - uncomment to re-enable
        /*
        try {
            // TEMPORARY: Force debug mode ON for Alliance Mobilization testing
            boolean debugMode = true; // profile.getConfig(EnumConfigurationKey.BOOL_DEBUG, Boolean.class);
            if (debugMode) {
                String timestamp = String.valueOf(System.currentTimeMillis());
                String filename = "AM_" + description + "_" + timestamp + ".png";
                logInfo("📸 Debug screenshot: " + filename);

                // Capture and save screenshot
                byte[] screenshotBytes = emuManager.captureScreenshotViaADB(EMULATOR_NUMBER);
                if (screenshotBytes != null && screenshotBytes.length > 0) {
                    // Save to screenshots folder
                    java.io.File screenshotsDir = new java.io.File("screenshots");
                    if (!screenshotsDir.exists()) {
                        screenshotsDir.mkdirs();
                    }

                    java.io.File screenshotFile = new java.io.File(screenshotsDir, filename);
                    java.nio.file.Files.write(screenshotFile.toPath(), screenshotBytes);
                    logInfo("Screenshot saved: " + screenshotFile.getAbsolutePath());
                } else {
                    logWarning("Screenshot capture returned empty data");
                }
            } else {
                logDebug("Debug screenshot skipped (debug mode off): " + description);
            }
        } catch (Exception e) {
            logWarning("Failed to capture debug screenshot: " + e.getMessage());
            e.printStackTrace();
        }
        */
    }

    // Helper method to visualize OCR area
    private void debugOCRArea(String description, DTOPoint topLeft, DTOPoint bottomRight, String ocrResult) {
        logDebug("═══ OCR Debug: " + description + " ═══");
        logDebug("  Area: (" + topLeft.getX() + "," + topLeft.getY() + ") → (" +
                 bottomRight.getX() + "," + bottomRight.getY() + ")");
        logDebug("  Width: " + (bottomRight.getX() - topLeft.getX()) + "px");
        logDebug("  Height: " + (bottomRight.getY() - topLeft.getY()) + "px");
        logDebug("  Result: '" + (ocrResult != null ? ocrResult : "null") + "'");
        logDebug("═══════════════════════════════════");
    }

    // Helper method to log template search results
    private void debugTemplateSearch(String templateName, DTOImageSearchResult result, int threshold) {
        if (result.isFound()) {
            logInfo("✅ Template FOUND: " + templateName + " at (" +
                   result.getPoint().getX() + "," + result.getPoint().getY() +
                   ") match: " + String.format("%.1f", result.getMatchPercentage()) + "% (threshold: " + threshold + "%)");
        } else {
            logInfo("❌ Template NOT FOUND: " + templateName + " (threshold: " + threshold + "%)");
        }
    }


    @Override
    public EnumStartLocation getRequiredStartLocation() {
        return EnumStartLocation.HOME;
    }

    @Override
    public boolean provideDailyMissionProgress() {
        return false; // Or true, depending on whether this task contributes to daily missions
    }
}
