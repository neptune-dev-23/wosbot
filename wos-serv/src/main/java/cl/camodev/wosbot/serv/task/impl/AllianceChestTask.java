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

public class AllianceChestTask extends DelayedTask {

	public AllianceChestTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment) {
		super(profile, heroRecruitment);
	}

	@Override
	protected void execute() {
		EmulatorManager emulator = EmulatorManager.getInstance();
		ServLogs logs = ServLogs.getServices();

		// Verificar si estamos en HOME o en WORLD
		boolean isHomeOrWorld = emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90).isFound() || emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90).isFound();

		if (!isHomeOrWorld) {
			logs.appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			emulator.tapBackButton(EMULATOR_NUMBER);
			return;
		}

		logs.appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Going to alliance chest");

		// Ir a la sección de cofres de alianza
		emulator.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult allianceChestResult = emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_CHEST_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
		if (!allianceChestResult.isFound()) {
			rescheduleTask();
			return;
		}

		emulator.tapAtRandomPoint(EMULATOR_NUMBER, allianceChestResult.getPoint(), allianceChestResult.getPoint());
		sleepTask(4000);

		// Abrir el cofre
		emulator.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(56, 375), new DTOPoint(320, 420));
		sleepTask(2000);

		// Buscar el botón de reclamar recompensas
		DTOImageSearchResult claimButton = emulator.searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_CHEST_LOOT_CLAIM_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
		if (claimButton.isFound()) {
			emulator.tapAtRandomPoint(EMULATOR_NUMBER, claimButton.getPoint(), claimButton.getPoint(), 10, 300);
			sleepTask(1000);
		}

		// Confirmar la acción
		emulator.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(410, 375), new DTOPoint(626, 420));
		sleepTask(2000);

		// Cerrar la ventana
		emulator.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(578, 1180), new DTOPoint(641, 1200), 10, 300);

		rescheduleTask();
	}

	/**
	 * Obtiene el tiempo de reprogramación y actualiza la tarea.
	 */
	private void rescheduleTask() {
		int offset = profile.getConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, Integer.class);
		LocalDateTime nextExecutionTime = LocalDateTime.now().plusHours(offset);

		this.reschedule(nextExecutionTime);
		ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.ALLIANCE_CHESTS, nextExecutionTime);

	}
}
