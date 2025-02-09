package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.utiles.ImageSearchUtil;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class NomadicMerchantTask extends DelayedTask {

	// Constantes de la tarea
	private static final String TASK_NAME = "Nomadic Merchant Task";

	// Coordenadas de los cuadrados en la pantalla
	private static final int[][] SQUARES = { { 40, 432 }, { 261, 432 }, { 481, 432 }, { 40, 741 }, { 261, 741 },
			{ 481, 741 } };

	// Coordenadas del botón de refresh
	private static final int[] REFRESH_BUTTON = { 500, 230 };

	// Nombres de los templates a buscar
	private static final String[] TEMPLATES = { "coal", "wood", "meat", "stone" };

	public NomadicMerchantTask(String taskName, LocalDateTime scheduledTime) {
		super(taskName, scheduledTime);
	}

	/**
	 * Método principal de la tarea, refactorizado para mayor legibilidad.
	 */
	@Override
	protected void execute() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Executing " + TASK_NAME);

		EmulatorManager.captureScreenshot("0");

		if (!isHomeScreenVisible()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "HOME NOT FOUND");
		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "HOME FOUND GOING TO SHOP");
			navigateToShop();
			processSquaresAndRefresh();
		}
	}

	/**
	 * Verifica si la pantalla HOME está visible.
	 */
	private boolean isHomeScreenVisible() {
		return ImageSearchUtil.buscarTemplate("/templates/city.png", 600, 1150, 100, 100).isFound();
	}

	/**
	 * Realiza los taps para navegar a la tienda.
	 */
	private void navigateToShop() {
		EmulatorManager.tapAtPoint(new DTOPoint(420, 1220));
		sleep(3000);
		EmulatorManager.tapAtPoint(new DTOPoint(70, 1220));
		sleep(3000);
	}

	/**
	 * Procesa cada uno de los cuadrados y verifica la presencia del botón refresh.
	 */
	private void processSquaresAndRefresh() {
		boolean continueTask = true;
		while (continueTask) {
			// Recorremos cada cuadrado utilizando un índice para el log
			for (int i = 0; i < SQUARES.length; i++) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Evaluating square " + i+1);
				processSquare(SQUARES[i]);
			}
			if (!isRefreshGemRefresh()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Refreshing...");
				EmulatorManager.tapAtPoint(new DTOPoint(600, 270));
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO,
						"Refresh button not found, finishing task.");
				continueTask = false;
				setRecurring(false);
			}
		}
	}

	/**
	 * Procesa un cuadrado: realiza la acción de tap si se encuentran templates o la
	 * acción VIP.
	 */
	private void processSquare(int[] square) {
		boolean reevaluate = true;
		while (reevaluate) {
			handleTemplateTaps(square);

			if (isVipFound(square)) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Button VIP found, tapping...");
				handleVipSquare(square);

				// Después de la acción VIP se vuelve a evaluar el mismo cuadrado.
				continue;
			} else {
				reevaluate = false;
				sleep(2000);
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
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, "Resource found, tapping...");
				EmulatorManager.tapAtPoint(new DTOPoint(square[0] + 100, square[1] + 220));
				sleep(1500);
			}
		}
	}

	/**
	 * Verifica si alguno de los templates (coal, wood, meat, stone) está presente
	 * en la región del cuadrado.
	 *
	 * @param square Coordenadas del cuadrado.
	 * @param width  Ancho de la región.
	 * @param height Altura de la región.
	 * @return true si se encuentra alguno de los templates, false en caso
	 *         contrario.
	 */
	private boolean isAnyTemplateFound(int[] square, int width, int height) {
		EmulatorManager.captureScreenshot("0");
		for (String templateName : TEMPLATES) {
			String resourcePath = "/templates/" + templateName + ".png";
			DTOImageSearchResult result = ImageSearchUtil.buscarTemplate(resourcePath, square[0], square[1], width,
					height);
			if (result.isFound()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifica si en el cuadrado se encuentra la plantilla VIP.
	 *
	 * @param square Coordenadas del cuadrado.
	 * @return true si se detecta VIP, false en caso contrario.
	 */
	private boolean isVipFound(int[] square) {
		EmulatorManager.captureScreenshot("0");
		return ImageSearchUtil.buscarTemplate("/templates/vip.png", square[0], square[1], 200, 250).isFound();
	}

	/**
	 * Ejecuta la secuencia de taps para la acción VIP.
	 *
	 * @param square Coordenadas del cuadrado donde se detectó VIP.
	 */
	private void handleVipSquare(int[] square) {
		EmulatorManager.tapAtPoint(new DTOPoint(square[0] + 100, square[1] + 220));
		sleep(1000);
		EmulatorManager.tapAtPoint(new DTOPoint(368, 830));
		sleep(1000);
		EmulatorManager.tapAtPoint(new DTOPoint(355, 788));
	}

	/**
	 * Verifica si el botón refresh está visible en la pantalla.
	 */
	private boolean isRefreshGemRefresh() {
		EmulatorManager.captureScreenshot("0");
		return ImageSearchUtil.buscarTemplate("/templates/refresh.png", REFRESH_BUTTON[0], REFRESH_BUTTON[1], 200, 70)
				.isFound();
	}

	/**
	 * Método auxiliar para pausar la ejecución.
	 *
	 * @param ms Cantidad de milisegundos a pausar.
	 */
	private void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
