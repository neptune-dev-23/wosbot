package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class AllianceTechTask extends DelayedTask {

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	private final static String TASK_NAME = "Alliance Tech";

	public AllianceTechTask(DTOProfiles list, LocalDateTime scheduledTime) {
		super(TASK_NAME, scheduledTime);
		this.profile = list;
		this.EMULATOR_NUMBER = list.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {

		// Buscar la plantilla de la pantalla HOME
		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "going alliance tech");
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
			sleepTask(3000);

			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(390, 895), new DTOPoint(649, 970));
			sleepTask(3000);

			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(330, 1000), new DTOPoint(390, 1070));
			sleepTask(3000);

			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(330, 1000), new DTOPoint(390, 1070));
			sleepTask(2000);

			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(400, 1000), new DTOPoint(618, 1056), 15, 300);

			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			sleepTask(200);
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			sleepTask(200);
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "rescheduling task in 60 minutes");
			this.reschedule(LocalDateTime.now().plusMinutes(60));

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, TASK_NAME, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}
		sleepTask(3000);

	}

	@Override
	public boolean isDailyTask() {
		return false;
	}

}
