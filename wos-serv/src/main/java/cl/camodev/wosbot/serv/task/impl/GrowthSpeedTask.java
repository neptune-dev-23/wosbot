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
import cl.camodev.wosbot.serv.task.DelayedTask;

public class GrowthSpeedTask extends DelayedTask {

    private final EmulatorManager emuManager = EmulatorManager.getInstance();
    private final ServLogs servLogs = ServLogs.getServices();

    public GrowthSpeedTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        DTOImageSearchResult homeResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
        DTOImageSearchResult worldResult = emuManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);

        if (homeResult.isFound() || worldResult.isFound()) {
            if (worldResult.isFound()) {
                emuManager.tapAtPoint(EMULATOR_NUMBER, worldResult.getPoint());
                sleepTask(4000);
            }

            servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Starting growth speed boost");

            // Click the small icon under the profile picture
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(18, 61));
            sleepTask(500);

            // Click growth tab
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(284, 60));
            sleepTask(500);

            // Click gathering speed button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(349, 215));
            sleepTask(500);

            // Click use button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(311, 295));
            sleepTask(500);

            // Click buy button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(306, 204));
            sleepTask(500);

            // Click confirm buy button
            emuManager.tapAtPoint(EMULATOR_NUMBER, new DTOPoint(200, 415));
            sleepTask(500);

            servLogs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Growth speed boost completed");

            // Reschedule task for 8 hours later
            this.reschedule(LocalDateTime.now().plusHours(8));

            // Go back to home
            emuManager.tapBackButton(EMULATOR_NUMBER);
            emuManager.tapBackButton(EMULATOR_NUMBER);
            emuManager.tapBackButton(EMULATOR_NUMBER);

        } else {
            servLogs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
            emuManager.tapBackButton(EMULATOR_NUMBER);
            this.reschedule(LocalDateTime.now().plusMinutes(5));
        }
    }
}
