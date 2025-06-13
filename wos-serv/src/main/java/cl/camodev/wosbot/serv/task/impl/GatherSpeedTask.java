package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

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

public class GatherSpeedTask extends DelayedTask {

    private final EmulatorManager emuManager = EmulatorManager.getInstance();
    private final ServLogs servLogs = ServLogs.getServices();

    public GatherSpeedTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
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

            servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Starting gather speed boost");

            // Click the small icon under the profile picture
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(40, 118));
            sleepTask(500);

            // Click gather tab
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(530, 122));
            sleepTask(500);

            // Click gathering speed button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(313, 406));
            sleepTask(500);

            // Click use button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(578, 568));
            sleepTask(500);

            // Click buy button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(585, 391));
            sleepTask(500);

            // Click confirm buy button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(382, 784));
            sleepTask(500);
            
            servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Gather speed boost completed");

            // Reschedule task for 8 hours later
            LocalDateTime nextSchedule = LocalDateTime.now().plusHours(8);
            this.reschedule(nextSchedule);
            ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, nextSchedule);

            // Go back to home
            emuManager.tapBackButton(EMULATOR_NUMBER);
            emuManager.tapBackButton(EMULATOR_NUMBER);
            emuManager.tapBackButton(EMULATOR_NUMBER);
        } else {
            servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
            emuManager.tapBackButton(EMULATOR_NUMBER);

            LocalDateTime retrySchedule = LocalDateTime.now().plusMinutes(5);
            this.reschedule(retrySchedule);
        }
    }
}
