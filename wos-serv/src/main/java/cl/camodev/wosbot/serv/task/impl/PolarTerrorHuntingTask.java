package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.utiles.UtilRally;
import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.almac.entity.DailyTask;
import cl.camodev.wosbot.almac.repo.DailyTaskRepository;
import cl.camodev.wosbot.almac.repo.IDailyTaskRepository;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;
import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.time.temporal.ChronoUnit;

import static cl.camodev.wosbot.console.enumerable.EnumTemplates.RALLY_BUTTON;

public class PolarTerrorHuntingTask extends DelayedTask {
    private final int refreshStaminaLevel = 180;
    private final int minStaminaLevel = 100;
    private final IDailyTaskRepository iDailyTaskRepository = DailyTaskRepository.getRepository();

    public PolarTerrorHuntingTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        logInfo("=== Starting Polar Terror Hunting Task ===");
      
        if (isBearRunning()) {
            LocalDateTime rescheduleTo = LocalDateTime.now().plusMinutes(30);
            logInfo("Bear Hunt is running, rescheduling for " + rescheduleTo);
            reschedule(rescheduleTo);
            return;
        }
        logDebug("Bear Hunt is not running, coninuing with Polar Terror Hunting Task");
        
        String flagString = profile.getConfig(EnumConfigurationKey.POLAR_TERROR_FLAG_STRING, String.class);
        int flagNumber = 0;
        int polarTerrorLevel = profile.getConfig(EnumConfigurationKey.POLAR_TERROR_LEVEL_INT, Integer.class);
        boolean limitedHunting = profile.getConfig(EnumConfigurationKey.POLAR_TERROR_MODE_STRING, String.class).equals("Limited (10)");
        boolean useFlag = false;

        if (flagString != null) {
            try {
                flagNumber = Integer.parseInt(flagString);
                useFlag = true;
            } catch (NumberFormatException e) {
                useFlag = false;
            }
        }

        if (profile.getConfig(EnumConfigurationKey.INTEL_BOOL, Boolean.class)
         && useFlag) {
            // Make sure intel isn't about to run
            DailyTask intel = iDailyTaskRepository.findByProfileIdAndTaskName(profile.getId(), TpDailyTaskEnum.INTEL);
            if (ChronoUnit.MINUTES.between(LocalDateTime.now(), intel.getNextSchedule()) < 5) {
                reschedule(LocalDateTime.now().plusMinutes(35)); // Reschedule in 35 minutes, after intel has run
                logWarning("Intel task is scheduled to run soon. Rescheduling Polar Hunt to run 30min after intel.");
                return;
            }
        }

        logInfo(String.format("Configuration: Level %d | %s Mode | Flag: %s", 
            polarTerrorLevel,
            limitedHunting ? "Limited (10 hunts)" : "Unlimited",
            useFlag ? "#" + flagString : "None"));

        // verify if there's enough stamina to hunt, if not, reschedule the task
        int currentStamina = getStaminaValueFromIntelScreen();

        if (currentStamina < minStaminaLevel) {
            LocalDateTime rescheduleTime = LocalDateTime.now().plusMinutes(staminaRegenerationTime(currentStamina,refreshStaminaLevel));
            reschedule(rescheduleTime);
            logWarning("Not enough stamina to do polar (Current: " + currentStamina + "). Rescheduling task in " + rescheduleTime + "minutes.");
            return;
        }

        if (!checkMarchesAvailable()) {
            logWarning("No marches available, rescheduling for in 5 minutes.");
            reschedule(LocalDateTime.now().plusMinutes(5));
            return;
        }

        if (limitedHunting) {
            if (!polarsRemaining(polarTerrorLevel)) {
                logWarning("No polars remaining for hunting...");
                return; // has already been rescheduled in the polarsRemaining command
            }
        }

        logInfo("Trying to launch rally now...");
        boolean success = launchPolarRally(polarTerrorLevel, useFlag, flagNumber);

        // if we exit loop without successful managed dispatch when a flag was required, log and return
        if (!success && useFlag) { // reason unknown. Maybe throw some errors so it can be clearer. Let's just try again later.
            logError("Failed to deploy march. Trying again in 5 minutes.");
            reschedule(LocalDateTime.now().plusMinutes(5));;
            return;
        }

        // if at least one march was sent without a flag, reschedule to run again immediately
        if (!useFlag && success) {
            reschedule(LocalDateTime.now());
        }

    }

    private boolean openRallyMenu() {
        // need to search for rally button
        DTOImageSearchResult rallyButton = null;
        int rallyAttempts = 0;
        while (rallyAttempts < 4) {
            rallyButton = emuManager.searchTemplate(EMULATOR_NUMBER, RALLY_BUTTON, 90);
            if (rallyButton.isFound()) {
                break;
            }
            rallyAttempts++;
            sleepTask(500);
        }

        if (!rallyButton.isFound()) {
            logDebug("Rally button not found.");
            sleepTask(500);
            return false;
        }

        // Confirm rally and choose flag if required
        tapPoint(rallyButton.getPoint());
        sleepTask(1000);
        return true;
    }

    private boolean clickDeployButton(int maxRetries) {
        for (int i = 0; i <= maxRetries; i++) {
            DTOImageSearchResult deploy = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DEPLOY_BUTTON, 90);
            if (!deploy.isFound()) {
                continue;
            }
            tapPoint(deploy.getPoint());
            sleepTask(2000);
            deploy = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.TROOPS_ALREADY_MARCHING, 90);
            if (!deploy.isFound()) {
                return true;
            }
            logDebug("Deploy still present, retry " + (i + 1) + " of " + maxRetries);
            sleepTask(1000);
        }
        logError("March deployment may have failed, deploy button still present.");
        return false;
    }

    private boolean launchPolarRally(int polarLevel, boolean useFlag, int flagNumber) {
        int maxRetries = 5;
        ensureCorrectScreenLocation(getRequiredStartLocation());

        logInfo("Navigating to polars menu");
        for (int i = 0; i < maxRetries; i++) {
            if (openPolarsMenu(polarLevel)) {break;}
        }

        logInfo("Navigating to rally menu");
        for (int i = 0; i < maxRetries; i++) {
            if (openRallyMenu()) {break;}
        }

        tapRandomPoint(new DTOPoint(275, 821), new DTOPoint(444, 856), 1, 400); // confirms rally time
        if (useFlag) {
            tapRandomPoint(UtilRally.getMarchFlagPoint(flagNumber), UtilRally.getMarchFlagPoint(flagNumber), 1, 200);
        }
        long travelTimeSeconds = 0;
        try {
            String timeStr = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(521, 1141), new DTOPoint(608, 1162));
            travelTimeSeconds = UtilTime.parseTimeToSeconds(timeStr) * 2;
        } catch (Exception e) {
            logError("Error parsing travel time: " + e.getMessage());
        }

        // Try to deploy and verify it disappears; retry a few times if needed
        boolean deployed = clickDeployButton(maxRetries);
        if (!deployed) {
            return false;
        }
        // No deploy button found after confirm: assume march sent
        logInfo("March deployed successfully.");

        // Deduct stamina for successful deployment
        int currentStamina = getStaminaValueFromIntelScreen();
        logInfo("Stamina decreased by 25. Current stamina: " + currentStamina);

        // If limitedHunting is true, only allow one march and return
        if (profile.getConfig(EnumConfigurationKey.POLAR_TERROR_MODE_STRING, String.class).equals("Limited (10)")) {
            logInfo("Limited hunting mode enabled - exiting after one successful march.");
            if (travelTimeSeconds > 0) {
                reschedule(LocalDateTime.now().plusSeconds(travelTimeSeconds).plusMinutes(5));
            } else {
                reschedule(LocalDateTime.now().plusMinutes(5));
            }
            return true;
        }

        if (useFlag && travelTimeSeconds > 0) {
                reschedule(LocalDateTime.now().plusSeconds(travelTimeSeconds).plusMinutes(5));
                return true;
        }

        if (!useFlag) {
            // Check if enough stamina remains to continue
            if (currentStamina <= minStaminaLevel) {
                logInfo("Stamina is at or below 100. Stopping deployment and rescheduling.");
                reschedule(LocalDateTime.now().plusMinutes(staminaRegenerationTime(currentStamina,refreshStaminaLevel)));
                return true;
            }

            // success but no flag: we want to send more marches in this run, keep looping
            sleepTask(1500);
            return launchPolarRally(polarLevel, useFlag, flagNumber); // recurse, will break once all polars are gone!
        }
        return true;
    }
    private boolean isBearRunning() {
        DTOImageSearchResult result = searchTemplateWithRetries(EnumTemplates.BEAR_HUNT_IS_RUNNING);
        return result.isFound();
    }

    private boolean openPolarsMenu(int polarLevel) {
        //navigate to a polar terror
        // Open search (magnifying glass)
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(25, 850), new DTOPoint(67, 898));
        sleepTask(2000);

        // Swipe left to find polar terror icon
        emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(40, 913), new DTOPoint(678, 913));
        sleepTask(500);

        // search the polar terror icon
        DTOImageSearchResult polarTerror = null;
        int attempts = 0;
        while (attempts < 4) {
            polarTerror = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.POLAR_TERROR_SEARCH_ICON, 90);
            if (polarTerror.isFound()) {
                break;
            }
            attempts++;
            logDebug(String.format("Searching for Polar Terror icon (attempt %d/4)", attempts));
            emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(40, 913), new DTOPoint(678, 913));
            sleepTask(500);
        }

        if (!polarTerror.isFound()) {
            logWarning("Failed to find the polar terrors.");
            return false;
        }
        //need to tap on the polar terror icon and check the current level selected
        tapPoint(polarTerror.getPoint());
        if (polarLevel != -1) {
            logInfo(String.format("Adjusting Polar Terror level to %d", polarLevel));
            emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(435, 1052), new DTOPoint(40, 1052)); // Swipe to level 1
            sleepTask(250);
            if (polarLevel > 1) {
                emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(487, 1055), new DTOPoint(487, 1055),
                    (polarLevel - 1), 150);
            }

        }
        // tap on search button
        logDebug("Tapping on search button...");
        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(301, 1200), new DTOPoint(412, 1229));
        sleepTask(4000);
        return true;
    }

    private boolean polarsRemaining(int polarLevel) {
        if (!openPolarsMenu(polarLevel)) {
            return false;
        }

        //need to search fot the magnifying glass icon to be sure we're on the search screen
        DTOImageSearchResult magnifyingGlass = null;
        int attempts = 0;
        while (attempts < 4) {
            magnifyingGlass = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.POLAR_TERROR_TAB_MAGNIFYING_GLASS_ICON, 90);
            if (magnifyingGlass.isFound()) {
                break;
            }
            attempts++;
            logDebug(String.format("Searching for magnifying glass icon (attempt %d/4)", attempts));
            sleepTask(500);
        }
        if (!magnifyingGlass.isFound()) {
            return false;
        }
        // need to scroll down a little bit and search for the remaining hunts "Special Rewards (n left)"
        tapPoint(magnifyingGlass.getPoint());
        sleepTask(2000);
        DTOImageSearchResult specialRewards = null;
        attempts = 0;
        while (attempts < 5) {
            specialRewards = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.POLAR_TERROR_TAB_SPECIAL_REWARDS, 90);
            if (specialRewards.isFound()) {
                //due limited mode is enabled, and there's no special rewards found, means there's no hunts left
                logWarning("No special rewards found, meaning there's no hunts left for today. Rescheduling task for reset");
                //lets add 15 minutes to let intel be processed
                reschedule(UtilTime.getGameReset().plusMinutes(15));
                return false;
            }
            attempts++;
            swipe(new DTOPoint(363, 1088), new DTOPoint(363, 1030));
            sleepTask(200);
        }
        // there are rallies available still, continue
        //navigate to world screen
        tapBackButton();
        sleepTask(500);
        return true;
    }

    private boolean checkMarchesAvailable() {
        // open active marches panel
        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(2, 550));
        sleepTask(500);
        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(340, 265));
        sleepTask(500);
        // OCR Search for an empty march
        try {
            for (int i = 0; i < 5; i++) { // search 10x for the OCR text
                String ocrSearchResult = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(10, 342), new DTOPoint(435, 772));
                Pattern idleMarchesPattern = Pattern.compile("idle");
                Matcher m = idleMarchesPattern.matcher(ocrSearchResult.toLowerCase());
                if (m.find()) {
                    logInfo("Idle marches detected, continuing with intel");
                    return true;
                } else {
                    logInfo("No idle marches detected, trying again (Attempt " + (i + 1) + "/5).");
                }
                sleepTask(100);
            }
        } catch (IOException | TesseractException e) {
            logDebug("OCR attempt failed: " + e.getMessage());
        }
        logInfo("No idle marches detected. ");
        return false;
    }

    @Override
    protected EnumStartLocation getRequiredStartLocation() {
        return EnumStartLocation.WORLD;
    }

}
