package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

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

public class GatherTask extends DelayedTask {
    private final GatherType gatherType;
    private final IDailyTaskRepository iDailyTaskRepository = DailyTaskRepository.getRepository();

    //@formatter:on
    private DTOPoint[][] queues = {
            {new DTOPoint(10, 342), new DTOPoint(435, 407), new DTOPoint(152, 378)},
            {new DTOPoint(10, 415), new DTOPoint(435, 480), new DTOPoint(152, 451)},
            {new DTOPoint(10, 488), new DTOPoint(435, 553), new DTOPoint(152, 524)},
            {new DTOPoint(10, 561), new DTOPoint(435, 626), new DTOPoint(152, 597)},
            {new DTOPoint(10, 634), new DTOPoint(435, 699), new DTOPoint(152, 670)},
            {new DTOPoint(10, 707), new DTOPoint(435, 772), new DTOPoint(152, 743)},
    };

    public GatherTask(DTOProfiles profile, TpDailyTaskEnum tpTask, GatherType gatherType) {
        super(profile, tpTask);
        this.gatherType = gatherType;
    }

    public static LocalDateTime parseRemaining(String timeStr) {
        if (timeStr == null) {
            return LocalDateTime.now();
        }
        // Removes line breaks and extra spaces
        timeStr = timeStr.replaceAll("\\r?\\n", "").trim();

        // The hh:mm:ss format is expected
        String[] parts = timeStr.split(":");
        if (parts.length != 3) {
            return LocalDateTime.now();
        }

        try {
            int hours = Integer.parseInt(parts[0].trim());
            int minutes = Integer.parseInt(parts[1].trim());
            int seconds = Integer.parseInt(parts[2].trim());

            LocalDateTime now = LocalDateTime.now();
            // The parsed time is added to the current time
            return now.plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
        } catch (NumberFormatException e) {
            // If an error occurs during parsing, LocalDateTime.now() is returned with an addition of 3 minutes
            return LocalDateTime.now().plusMinutes(3);
        }
    }

    @Override
    protected void execute() {
        // Check if GatherSpeedTask is not processed yet
        if (profile.getConfig(EnumConfigurationKey.GATHER_SPEED_BOOL, Boolean.class)
                && !isGatherSpeedTaskReadyForGathering()) {
            logInfo("Waiting for GatherSpeedTask to be processed. Checking again in 2 minutes.");
            reschedule(LocalDateTime.now().plusMinutes(2)); // Check again in 2 minutes
            return;
        }

        DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER,
                EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 90);
        DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER,
                EnumTemplates.GAME_HOME_WORLD.getTemplate(), 90);

        if (homeResult.isFound() || worldResult.isFound()) {
            if (worldResult.isFound()) {
                emuManager.tapAtPoint(EMULATOR_NUMBER, worldResult.getPoint());
                sleepTask(4000);
            }

            // Check active marches
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(2, 550));
            sleepTask(500);
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(340, 265));
            sleepTask(500);
            logInfo("Looking for an active " + gatherType + " gathering march.");

            // Get active march queue setting and calculate search region
            int activeMarchQueues = profile.getConfig(EnumConfigurationKey.GATHER_ACTIVE_MARCH_QUEUE_INT,
                    Integer.class);
            int maxY = queues[Math.min(activeMarchQueues - 1, queues.length - 1)][1].getY(); // Use the Y coordinate of
            // the last active queue

            DTOImageSearchResult resource = emuManager.searchTemplate(EMULATOR_NUMBER, gatherType.getTemplate(), new DTOPoint(10,
                    342), new DTOPoint(415, maxY), 90);

            if (resource.isFound()) {
                logInfo("Active " + gatherType + " gathering march found. Getting remaining time...");
                int index = getIndex(resource.getPoint());
                if (index != -1) {
                    try {
                        String time = emuManager.ocrRegionText(EMULATOR_NUMBER, queues[index][2],
                                new DTOPoint(queues[index][2].getX() + 140, queues[index][2].getY() + 19));
                        LocalDateTime nextSchedule = parseRemaining(time).plusMinutes(2);
                        logInfo("Gathering is in progress. Rescheduling for: " + nextSchedule);
                        this.reschedule(nextSchedule);
                    } catch (Exception e) {
                        logError("Failed to parse remaining time for the active gather march. Rescheduling in 5 minutes. " + e.getMessage());
                        reschedule(LocalDateTime.now().plusMinutes(5));
                    }
                } else {
                    logWarning("Could not determine the queue index for the active gather march. Rescheduling in 5 minutes.");
                    reschedule(LocalDateTime.now().plusMinutes(5));
                }
                // Go back to home screen
                emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
                emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(464, 551));
            } else {
                logInfo("No active gathering march found for " + gatherType + ". Starting a new one.");
                // Go back to home screen before starting search
                emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
                emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(464, 551));
                sleepTask(1000);

                // Ensure we are on the home screen before tapping world view
                homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 90);
                if (homeResult.isFound()) {
                    emuManager.tapAtPoint(EMULATOR_NUMBER, homeResult.getPoint());
                    sleepTask(3000);
                }

                // Open search (magnifying glass)
                emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(25, 850), new DTOPoint(67, 898));
                sleepTask(2000);

                // Swipe left to find resource tiles
                emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(678, 913), new DTOPoint(40, 913));
                sleepTask(500);

                // Find the correct resource tile
                logDebug("Swiping to find the correct resource tile.");
                DTOImageSearchResult tile = null;
                int attempts = 0;
                while (attempts < 4) {
                    tile = emuManager.searchTemplate(EMULATOR_NUMBER, gatherType.getTile(), 90);
                    if (tile.isFound()) {
                        break;
                    }
                    attempts++;
                    logDebug("Swiping to find the correct resource tile, attempt " + attempts);
                    emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(678, 913), new DTOPoint(40, 913));
                    sleepTask(500);
                }

                if (tile.isFound()) {
                    logInfo("Resource tile found: " + gatherType.getTile());
                    emuManager.tapAtPoint(EMULATOR_NUMBER, tile.getPoint());
                    sleepTask(500);

                    // Set resource level
                    int level = profile.getConfig(gatherType.getConfig(), Integer.class);
                    logInfo("Setting resource level to " + level + ".");
                    emuManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(435, 1052), new DTOPoint(40, 1052)); // Swipe to level 1
                    sleepTask(300);
                    if (level > 1) {
                        emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(487, 1055), new DTOPoint(487, 1055),
                                (level - 1), 150);
                    }

                    DTOImageSearchResult tick = emuManager.searchTemplate(EMULATOR_NUMBER,
                            EnumTemplates.GAME_HOME_SHORTCUTS_FARM_TICK.getTemplate(), 90);
                    if (!tick.isFound()) {
                        emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(183, 1140));
                    }

                    // Click Search
                    logInfo("Searching for the tile...");
                    emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(301, 1200), new DTOPoint(412, 1229));
                    sleepTask(3000);

                    // Click Gather button on the map
                    DTOImageSearchResult gather = emuManager.searchTemplate(EMULATOR_NUMBER,
                            EnumTemplates.GAME_HOME_SHORTCUTS_FARM_GATHER.getTemplate(), 90);
                    if (gather.isFound()) {
                        emuManager.tapAtPoint(EMULATOR_NUMBER, gather.getPoint());
                        sleepTask(1000);

                        boolean removeHeros = profile.getConfig(EnumConfigurationKey.GATHER_REMOVE_HEROS_BOOL, Boolean.class);
                        if (removeHeros) {
                            // Remove 2nd and 3rd heroes
                            logInfo("Removing default heroes from the march.");
                            List<DTOImageSearchResult> results = emuManager.searchTemplates(EMULATOR_NUMBER, EnumTemplates.RALLY_REMOVE_HERO_BUTTON.getTemplate(), 90, 3);

                            results.sort(Comparator.comparingInt(r -> r.getPoint().getX()));

                            for (int i = 1; i < results.size(); i++) {
                                emuManager.tapAtPoint(EMULATOR_NUMBER, results.get(i).getPoint());
                                sleepTask(300);
                            }

                        }

                        // Click gather button on tile
                        logInfo("Initiating gather march.");
                        DTOImageSearchResult deployButton = emuManager.searchTemplate(EMULATOR_NUMBER,
                                EnumTemplates.GATHER_DEPLOY_BUTTON.getTemplate(), 90); // Deploy button
                        if (deployButton.isFound()) {
                            emuManager.tapAtPoint(EMULATOR_NUMBER, deployButton.getPoint());
                            sleepTask(1000);

                            // Check if another march is already on the way
                            DTOImageSearchResult march = emuManager.searchTemplate(EMULATOR_NUMBER,
                                    EnumTemplates.TROOPS_ALREADY_MARCHING.getTemplate(), 90);
                            if (march.isFound()) {
                                logWarning("The tile is already being gathered by another player. Rescheduling task.");
                                emuManager.tapBackButton(EMULATOR_NUMBER);
                                emuManager.tapBackButton(EMULATOR_NUMBER);
                                reschedule(LocalDateTime.now().plusMinutes(1)); // Try again soon for a new tile
                            } else {
                                logInfo("March started successfully. Rescheduling the next check in 5 minutes.");
                                reschedule(LocalDateTime.now().plusMinutes(5));
                            }
                        } else {
                             logError("The 'March' button was not found. Aborting and rescheduling in 5 minutes.");
                             emuManager.tapBackButton(EMULATOR_NUMBER);
                             reschedule(LocalDateTime.now().plusMinutes(5));
                        }

                    } else {
                        logWarning("The 'Gather' button on the map was not found. The tile might be occupied. Rescheduling in 5 minutes.");
                        emuManager.tapBackButton(EMULATOR_NUMBER);
                        reschedule(LocalDateTime.now().plusMinutes(5));
                    }
                } else {
                    logError("The resource tile was not found after multiple swipes. Aborting and rescheduling in 15 minutes.");
                    emuManager.tapBackButton(EMULATOR_NUMBER);
                    reschedule(LocalDateTime.now().plusMinutes(15)); // Wait longer if tiles can't be found
                }
            }

        } else {
            logWarning("Not on the home or world screen. Tapping the back button and rescheduling.");
            emuManager.tapBackButton(EMULATOR_NUMBER);
            reschedule(LocalDateTime.now());
        }
    }

    public int getIndex(DTOPoint point) {
        // Get active march queue setting and limit the search to only active queues
        int activeMarchQueues = profile.getConfig(EnumConfigurationKey.GATHER_ACTIVE_MARCH_QUEUE_INT, Integer.class);
        int maxQueues = Math.min(activeMarchQueues, queues.length);

        for (int i = 0; i < maxQueues; i++) {
            // Get the range limits (to ensure correct order, we use Math.min and Math.max)
            int minX = Math.min(queues[i][0].getX(), queues[i][1].getX());
            int maxX = Math.max(queues[i][0].getX(), queues[i][1].getX());
            int minY = Math.min(queues[i][0].getY(), queues[i][1].getY());
            int maxY = Math.max(queues[i][0].getY(), queues[i][1].getY());

            // Check if the point is within the limits
            if (point.getX() >= minX && point.getX() <= maxX && point.getY() >= minY && point.getY() <= maxY) {
                return i; // Returns the index of the found pair
            }
        }
        return -1; // Returns -1 if the point is not in any of the ranges
    }

    private boolean isGatherSpeedTaskReadyForGathering() {
        try {
            DailyTask gatherSpeedTask = iDailyTaskRepository.findByProfileIdAndTaskName(profile.getId(),
                    TpDailyTaskEnum.GATHER_BOOST);

            if (gatherSpeedTask == null) {
                // GatherSpeedTask has never been executed, so gathering should wait
                logDebug("GatherSpeedTask has never been executed; waiting is required.");
                return false;
            }

            LocalDateTime nextSchedule = gatherSpeedTask.getNextSchedule();
            if (nextSchedule == null) {
                // If there's no next schedule, check again in 5 minutes
                logDebug("GatherSpeedTask has no next schedule; waiting is required.");
                return false;
            }

            // Check if the next schedule is more than 10 minutes from now
            long minutesUntilNextSchedule = ChronoUnit.MINUTES.between(LocalDateTime.now(), nextSchedule);

            // FIX: Sometimes for whatever reason, ServScheduler doesn't update the next
            // schedule correctly
            // Due to this error, minutesUntilNextSchedule can be less than 0 even though
            // task already run
            // We will skip if its less than 0 to make sure gathering can start
            if (minutesUntilNextSchedule > 0 && minutesUntilNextSchedule < 5) {
                logDebug("The next GatherSpeedTask is in " + minutesUntilNextSchedule + " minutes; waiting is required.");
                return false;
            } else {
                logDebug("The next GatherSpeedTask is in " + minutesUntilNextSchedule
                        + " minutes; gathering can start.");
                return true;
            }

        } catch (Exception e) {
            logError("Error checking GatherSpeedTask status: " + e.getMessage() + ". Waiting is required.");
            return false; // Wait for 2 minutes
        }
    }

    @Override
    protected Object getDistinctKey() {
        return gatherType;
    }

    @Override
    public boolean provideDailyMissionProgress() {
        return true;
    }

    //@formatter:off
	public enum GatherType {
		MEAT( EnumTemplates.GAME_HOME_SHORTCUTS_MEAT, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_MEAT, EnumConfigurationKey.GATHER_MEAT_LEVEL_INT),
		WOOD( EnumTemplates.GAME_HOME_SHORTCUTS_WOOD, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_WOOD, EnumConfigurationKey.GATHER_WOOD_LEVEL_INT),
		COAL( EnumTemplates.GAME_HOME_SHORTCUTS_COAL, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_COAL, EnumConfigurationKey.GATHER_COAL_LEVEL_INT),
		IRON( EnumTemplates.GAME_HOME_SHORTCUTS_IRON, EnumTemplates.GAME_HOME_SHORTCUTS_FARM_IRON, EnumConfigurationKey.GATHER_IRON_LEVEL_INT);


		EnumTemplates template;
		EnumTemplates tile;
		EnumConfigurationKey level;

		GatherType(EnumTemplates enumTemplate,EnumTemplates tile, EnumConfigurationKey level) {
		this.template = enumTemplate;
		this.tile = tile;
		this.level = level;
		}

		public String getTemplate() {
            return template.getTemplate();
		}

		public String getTile() {
            return tile.getTemplate();
		}

		public EnumConfigurationKey getConfig() {
            return level;
        }


	}

}
