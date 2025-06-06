package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

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

public class AllianceHelpTask extends DelayedTask {
	private EmulatorManager emulator = EmulatorManager.getInstance();
	private ServLogs logs = ServLogs.getServices();

	public AllianceHelpTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {

		// Verificar si estamos en HOME o en WORLD
		boolean isHomeOrWorld = emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90).isFound() || emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90).isFound();

		if (!isHomeOrWorld) {
			logs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			emulator.tapBackButton(EMULATOR_NUMBER);
			return;
		}

		logs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to alliance help");

		// Ir a la sección de cofres de alianza
		emulator.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult allianceChestResult = emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_HELP_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
		if (!allianceChestResult.isFound()) {
			logs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Alliance help button not found");
			rescheduleTask();
			return;
		}
		logs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Alliance help button found");
		emulator.tapAtRandomPoint(EMULATOR_NUMBER, allianceChestResult.getPoint(), allianceChestResult.getPoint());

		sleepTask(2000);

		DTOImageSearchResult allianceHelpResult = emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_HELP_REQUESTS.getTemplate(), 0, 0, 720, 1280, 90);
		if (allianceHelpResult.isFound()) {
			logs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Helping alliance members");
			emulator.tapAtRandomPoint(EMULATOR_NUMBER, allianceHelpResult.getPoint(), allianceHelpResult.getPoint());

		} else {
			logs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Alliance help requests not found");
		}
		emulator.tapBackButton(EMULATOR_NUMBER);
		sleepTask(100);
		emulator.tapBackButton(EMULATOR_NUMBER);

		rescheduleTask();
	}

	/**
	 * Obtiene el tiempo de reprogramación y actualiza la tarea.
	 */
	private void rescheduleTask() {
		int offset = profile.getConfig(EnumConfigurationKey.ALLIANCE_HELP_REQUESTS_OFFSET_INT, Integer.class);
		LocalDateTime nextExecutionTime = LocalDateTime.now().plusHours(offset);

		this.reschedule(nextExecutionTime);
		ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.ALLIANCE_HELP, nextExecutionTime);

	}

}
