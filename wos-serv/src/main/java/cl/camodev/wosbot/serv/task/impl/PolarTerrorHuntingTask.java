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
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.EnumStartLocation;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static cl.camodev.wosbot.console.enumerable.EnumTemplates.RALLY_BUTTON;

public class PolarTerrorHuntingTask extends DelayedTask {
    private final IDailyTaskRepository iDailyTaskRepository = DailyTaskRepository.getRepository();

    public PolarTerrorHuntingTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        logInfo("=== Starting Polar Terror Hunting Task ===");
        
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
                ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, LocalDateTime.now().plusMinutes(2));
                return;
            }
        }

        logInfo(String.format("Configuration: Level %d | %s Mode | Flag: %s", 
            polarTerrorLevel,
            limitedHunting ? "Limited (10 hunts)" : "Unlimited",
            useFlag ? "#" + flagString : "None"));

        // verify if there's enough stamina to hunt, if not, reschedule the task

        ensureOnIntelScreen();
        Integer currentStamina = readNumberValue(new DTOPoint(582, 23), new DTOPoint(672, 55));
        tapBackButton();

        if (currentStamina == null) {
            logWarning("Failed to read current stamina value. Retrying in 5 minutes.");
            this.reschedule(LocalDateTime.now().plusMinutes(5));
            return;
        }

        if (currentStamina < 100) {
            LocalDateTime rescheduleTime = LocalDateTime.now().plusMinutes(staminaRegenerationTime(currentStamina,120));
            reschedule(rescheduleTime);
            int waitMinutes = staminaRegenerationTime(currentStamina, 120);
            logInfo(String.format("Insufficient stamina for Polar Terror (Current: %d/120). Waiting %d minutes for regeneration.",
                currentStamina, waitMinutes));
            return;
        }

        if (limitedHunting) {
            //need to verify if there's enought daily hunts left
            //navigate to a polar terror
            // Open search (magnifying glass)
            tapRandomPoint(new DTOPoint(25, 850), new DTOPoint(67, 898));
            sleepTask(2000);

            // Swipe left to find polar terror icon
            swipe(new DTOPoint(40, 913), new DTOPoint(678, 913));
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
                swipe(new DTOPoint(40, 913), new DTOPoint(678, 913));
                sleepTask(500);
            }

            if (polarTerror.isFound()) {

                //need to tap on the polar terror icon and check the current level selected
                tapPoint(polarTerror.getPoint());
                sleepTask(1000);

                logDebug(String.format("Adjusting Polar Terror level to %d", polarTerrorLevel));
                swipe(new DTOPoint(435, 1052), new DTOPoint(40, 1052)); // Swipe to level 1
                sleepTask(250);
                if (polarTerrorLevel > 1) {
                    tapRandomPoint(new DTOPoint(487, 1055), new DTOPoint(487, 1055),
                            (polarTerrorLevel - 1), 150);
                }

                tapRandomPoint(new DTOPoint(301, 1200), new DTOPoint(412, 1229));
                sleepTask(4000);

                //need to search fot the magnifying glass icon to be sure we're on the search screen
                DTOImageSearchResult magnifyingGlass = null;
                attempts = 0;
                while (attempts < 4) {
                    magnifyingGlass = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.POLAR_TERROR_TAB_MAGNIFYING_GLASS_ICON, 90);
                    if (magnifyingGlass.isFound()) {
                        break;
                    }
                    attempts++;
                    logDebug(String.format("Searching for magnifying glass icon (attempt %d/4)", attempts));

                    sleepTask(500);
                }

                if (magnifyingGlass.isFound()) {
                    // need to scroll down a little bit and search for the remaining hunts "Special Rewards (n left)"
                    tapPoint(magnifyingGlass.getPoint());
                    sleepTask(2000);
                    DTOImageSearchResult specialRewards = null;
                    attempts = 0;
                    while (attempts < 5) {
                        specialRewards = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.POLAR_TERROR_TAB_SPECIAL_REWARDS, 90);
                        if (specialRewards.isFound()) {
                            break;
                        }
                        attempts++;
                        swipe(new DTOPoint(363, 1088), new DTOPoint(363, 1030));
                        sleepTask(500);
                    }

                    if (specialRewards.isFound()) {
                        //due limited mode is enabled, and there's no special rewards found, means there's no hunts left
                        logInfo("No daily hunts remaining. Rescheduling task for after game reset (+15 minutes).");
                        //lets add 15 minutes to let intel be processed
                        reschedule(UtilTime.getGameReset().plusMinutes(15));
                        return;
                    }


                    sleepTask(1500);
                    //navigate to world screen
                    tapBackButton();
                    sleepTask(500);
                }

            }

        }

        // if im here means there's enough stamina , and there's hunts available indiscriminately of the mode
        //need to check if there's a queues available (TODO) lest do this without checking for now 💀

        int dispatchAttempts = 0;
        int maxDispatchAttempts = 5;
        boolean anyDispatched = false;

        while (dispatchAttempts < maxDispatchAttempts) {

            ensureCorrectScreenLocation(getRequiredStartLocation());

            // Open search (magnifying glass)
            tapRandomPoint(new DTOPoint(25, 850), new DTOPoint(67, 898));
            sleepTask(2000);

            // Swipe left to find polar terror icon
            swipe(new DTOPoint(40, 913), new DTOPoint(678, 913));
            sleepTask(500);

            // search the polar terror icon
            DTOImageSearchResult polarTerror = null;
            int findAttempts = 0;
            while (findAttempts < 4) {
                polarTerror = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.POLAR_TERROR_SEARCH_ICON, 90);
                if (polarTerror.isFound()) {
                    break;
                }
                findAttempts++;
                logDebug("Swiping to find the correct tile, attempt " + findAttempts);
                swipe(new DTOPoint(40, 913), new DTOPoint(678, 913));
                sleepTask(500);
            }

            if (!polarTerror.isFound()) {
                logDebug("Polar terror icon not found, attempt " + (dispatchAttempts + 1));
                dispatchAttempts++;
                sleepTask(500);
                continue;
            }

            tapPoint(polarTerror.getPoint());
            sleepTask(300);

            logInfo("Setting polar terror level to " + polarTerrorLevel + ".");
            swipe(new DTOPoint(435, 1052), new DTOPoint(40, 1052)); // Swipe to level 1
            sleepTask(250);
            if (polarTerrorLevel > 1) {
                tapRandomPoint(new DTOPoint(487, 1055), new DTOPoint(487, 1055),
                        (polarTerrorLevel - 1), 150);
            }

            // tap on search button
            tapRandomPoint(new DTOPoint(301, 1200), new DTOPoint(412, 1229));
            sleepTask(4000);

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
                logDebug("Rally button not found, attempt " + (dispatchAttempts + 1));
                dispatchAttempts++;
                sleepTask(500);
                continue;
            }

            // Confirm rally and choose flag if required
            tapPoint(rallyButton.getPoint());
            sleepTask(1000);
            tapRandomPoint(new DTOPoint(275, 821), new DTOPoint(444, 856), 1, 400);
            if (useFlag) {
                tapPoint(UtilRally.getMarchFlagPoint(flagNumber));
            }
            long travelTimeSeconds = 0;
            try {
                String timeStr = emuManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(521, 1141), new DTOPoint(608, 1162));
                travelTimeSeconds = UtilTime.parseTimeToSeconds(timeStr) * 2;
            } catch (Exception e) {
                logError("Error parsing travel time: " + e.getMessage());
            }

            // Try to deploy and verify it disappears; retry a few times if needed
            DTOImageSearchResult deploy = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.DEPLOY_BUTTON, 90);
            if (deploy.isFound()) {
                int deployRetry = 0;
                int maxDeployRetries = 3;
                boolean deployed = false;
                while (deployRetry < maxDeployRetries) {
                    tapPoint(deploy.getPoint());
                    sleepTask(2000);
                    deploy = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.TROOPS_ALREADY_MARCHING, 90);
                    if (!deploy.isFound()) {
                        deployed = true;
                        break;
                    }
                    deployRetry++;
                    logDebug(String.format("Verifying troop deployment (attempt %d/%d)", deployRetry + 1, maxDeployRetries));
                    sleepTask(1000);
                }

                if (!deployed) {
                    logError("March deployment may have failed, deploy button still present. Attempt " + (dispatchAttempts + 1) + " of " + maxDispatchAttempts + ".");
                    dispatchAttempts++;
                    sleepTask(1000);
                    continue;
                } else {
                    logInfo(String.format("✓ March deployed successfully to Level %d Polar Terror%s",
                        polarTerrorLevel, useFlag ? " with Flag #" + flagString : ""));
                    anyDispatched = true;

                    // Deduct stamina for successful deployment
                    // TODO: adjust via OCR
                    currentStamina -= 25;
                    logDebug(String.format("Stamina update: %d → %d (-25)", currentStamina + 25, currentStamina));

                    // If limitedHunting is true, only allow one march and return
                    if (limitedHunting) {
                        logInfo("Limited hunting mode: Daily hunt completed. Rescheduling for march return.");
                        if (travelTimeSeconds > 0) {
                            reschedule(LocalDateTime.now().plusSeconds(travelTimeSeconds).plusMinutes(5));
                        } else {
                            reschedule(LocalDateTime.now().plusMinutes(5));
                        }
                        return;
                    }

                    if (useFlag) {
                        if (travelTimeSeconds > 0) {
                            reschedule(LocalDateTime.now().plusSeconds(travelTimeSeconds).plusMinutes(5));
                        }
                        return;
                    } else {
                        // Check if enough stamina remains to continue
                        if (currentStamina <= 100) {
                            logInfo("Stamina is at or below 100. Stopping deployment and rescheduling.");
                            reschedule(LocalDateTime.now().plusMinutes(staminaRegenerationTime(currentStamina,120)));
                            return;
                        }

                        // success but no flag: we want to send more marches in this run, keep looping
                        dispatchAttempts++;
                        sleepTask(1500);
                        continue;
                    }
                }
            } else {
                // No deploy button found after confirm: assume march sent
                logInfo("Deploy button not found after confirming rally. Assuming march sent.");
                anyDispatched = true;

                // Deduct stamina for successful deployment
                // TODO: adjust via OCR
                currentStamina -= 25;
                logInfo("Stamina decreased by 25. Current stamina: " + currentStamina);

                // If limitedHunting is true, only allow one march and return
                if (limitedHunting) {
                    logInfo("Limited hunting mode enabled - exiting after one successful march.");
                    if (travelTimeSeconds > 0) {
                        reschedule(LocalDateTime.now().plusSeconds(travelTimeSeconds).plusMinutes(5));
                    } else {
                        reschedule(LocalDateTime.now().plusMinutes(5));
                    }
                    return;
                }

                if (useFlag) {
                    if (travelTimeSeconds > 0) {
                        reschedule(LocalDateTime.now().plusSeconds(travelTimeSeconds).plusMinutes(5));
                    }
                    return;
                } else {
                    // Check if enough stamina remains to continue
                    if (currentStamina <= 100) {
                        logInfo("Stamina is at or below 100. Stopping deployment and rescheduling.");
                        reschedule(LocalDateTime.now().plusMinutes(staminaRegenerationTime(currentStamina,120)));
                        return;
                    }

                    dispatchAttempts++;
                    sleepTask(500);
                    continue;
                }
            }
        }

        // if we exit loop without successful managed dispatch when a flag was required, log and return
        if (!anyDispatched && useFlag) {
            logError("Failed to deploy march after " + maxDispatchAttempts + " attempts.");
            return;
        }

        // if at least one march was sent without a flag, reschedule to run again immediately
        if (!useFlag && anyDispatched) {
            reschedule(LocalDateTime.now().plusMinutes(2));
            return;
        }

    }

    @Override
    protected EnumStartLocation getRequiredStartLocation() {
        return EnumStartLocation.WORLD;
    }

    private int staminaRegenerationTime(int currentStamina, int targetStamina) {
        if (currentStamina >= targetStamina) {
            return 0;
        }
        int staminaNeeded = targetStamina - currentStamina;
        return staminaNeeded * 5; // 1 stamina every 5 minutes
    }
}
