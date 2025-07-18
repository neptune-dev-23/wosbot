package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.utiles.UtilTime;
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

public class NomadicMerchantTask extends DelayedTask {

	private final int[][] SQUARES = { { 40, 432 }, { 261, 432 }, { 481, 432 }, { 40, 741 }, { 261, 741 }, { 481, 741 } };
	private final int[] REFRESH_BUTTON = { 500, 230 };
	private final EnumTemplates[] TEMPLATES = { EnumTemplates.NOMADIC_MERCHANT_COAL, EnumTemplates.NOMADIC_MERCHANT_MEAT, EnumTemplates.NOMADIC_MERCHANT_STONE, EnumTemplates.NOMADIC_MERCHANT_WOOD };
	private boolean isVipEnabled = true;

	public NomadicMerchantTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	/**
	 * Método principal de la tarea, refactorizado para mayor legibilidad.
	 */
	@Override
	protected void execute() {

		// Buscar la plantilla de la pantalla HOME
		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(),  90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(),  90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "HOME FOUND GOING TO SHOP");
			EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(420, 1220));
			sleepTask(3000);
			EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(70, 1220));
			sleepTask(3000);
			processSquaresAndRefresh();
			ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.NOMADIC_MERCHANT, UtilTime.getGameReset());
			this.reschedule(UtilTime.getGameReset());

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "HOME NOT FOUND");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}
		sleepTask(3000);

	}

	/**
	 * Realiza los taps para navegar a la tienda.
	 */

	/**
	 * Procesa cada uno de los cuadrados y verifica la presencia del botón refresh.
	 */
	private void processSquaresAndRefresh() {
		boolean continueTask = true;
		while (continueTask) {
			// Recorremos cada cuadrado utilizando un índice para el log
			for (int i = 0; i < SQUARES.length; i++) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Evaluating square " + (i + 1));
				processSquare(SQUARES[i]);
			}
			if (!isRefreshGemRefresh()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Refreshing...");
				EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(600, 270));
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Refresh button not found, finishing task.");
				continueTask = false;
				EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			}
		}
	}

	/**
	 * Procesa un cuadrado: realiza la acción de tap si se encuentran templates o la acción VIP.
	 */
	private void processSquare(int[] square) {
		boolean reevaluate = true;
		while (reevaluate) {
			handleTemplateTaps(square);

			if (isVipEnabled) {

				if (isVipFound(square)) {
					ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Button VIP found, tapping...");
					handleVipSquare(square);
					sleepTask(2000);

					continue;
				} else {
					reevaluate = false;
					sleepTask(2000);
				}
			} else {
				reevaluate = false;
				sleepTask(2000);
			}

		}
	}

	/**
	 * Realiza taps en el cuadrado mientras se detecten alguno de los templates.
	 */
	private void handleTemplateTaps(int[] square) {
		if (isAnyTemplateFound(square, 200, 250)) {
			// Repetir taps hasta que ya no se detecte ninguno de los templates
			while (isAnyTemplateFound(square, 200, 250)) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Resource found, tapping...");
				EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(square[0] + 100, square[1] + 220));
				sleepTask(1500);
			}
		}
	}

	/**
	 * Verifica si alguno de los templates (coal, wood, meat, stone) está presente en la región del cuadrado.
	 *
	 * @param square Coordenadas del cuadrado.
	 * @param width  Ancho de la región.
	 * @param height Altura de la región.
	 * @return true si se encuentra alguno de los templates, false en caso contrario.
	 */
	private boolean isAnyTemplateFound(int[] square, int width, int height) {
		for (EnumTemplates templateName : TEMPLATES) {

			DTOImageSearchResult result = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, templateName.getTemplate(), square[0], square[1], width, height, 90);
			if (result.isFound()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Resource found: " + templateName.name());
				return true;
			}
		}
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Resource not found, skipping...");
		return false;

	}

	/**
	 * Verifica si en el cuadrado se encuentra la plantilla VIP.
	 *
	 * @param square Coordenadas del cuadrado.
	 * @return true si se detecta VIP, false en caso contrario.
	 */
	private boolean isVipFound(int[] square) {
		return EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.NOMADIC_MERCHANT_VIP.getTemplate(), square[0], square[1], 200, 250, 90).isFound();
	}

	/**
	 * Ejecuta la secuencia de taps para la acción VIP.
	 *
	 * @param square Coordenadas del cuadrado donde se detectó VIP.
	 */
	private void handleVipSquare(int[] square) {
		EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(square[0] + 100, square[1] + 220));
		sleepTask(1000);
		EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(368, 830));
		sleepTask(1000);
		EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(355, 788));
	}

	/**
	 * Verifica si el botón refresh está visible en la pantalla.
	 */
	private boolean isRefreshGemRefresh() {

		return EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.NOMADIC_MERCHANT_REFRESH.getTemplate(), REFRESH_BUTTON[0], REFRESH_BUTTON[1], 200, 70, 90).isFound();
	}

}
