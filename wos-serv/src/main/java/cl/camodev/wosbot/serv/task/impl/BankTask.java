package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.sourceforge.tess4j.TesseractException;

public class BankTask extends DelayedTask {

	private EmulatorManager emulatorManager = EmulatorManager.getInstance();

	public BankTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
		super(profile, tpTask);
	}

	@Override
	protected void execute() {
		int attempt = 0;
		int bankDelay = profile.getConfig(EnumConfigurationKey.INT_BANK_DELAY, null);

		while (attempt < 5) {
			// Verificar si estamos en la pantalla de inicio
			DTOImageSearchResult homeResult = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
			DTOImageSearchResult worldResult = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);

			if (homeResult.isFound() || worldResult.isFound()) {
				// Buscar el botón de ofertas (Deals)
				DTOImageSearchResult dealsResult = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.HOME_DEALS_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
				if (!dealsResult.isFound())
					return;

				emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, dealsResult.getPoint(), dealsResult.getPoint());
				sleepTask(2000);
				emulatorManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(630, 143), new DTOPoint(2, 128));

				// Buscar la opción de banco dentro de eventos
				DTOImageSearchResult bankResult = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.EVENTS_DEALS_BANK.getTemplate(), 0, 0, 720, 1280, 90);
				if (!bankResult.isFound())
					return;

				emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, bankResult.getPoint(), bankResult.getPoint());

				sleepTask(2000);
				DTOImageSearchResult bankDepositResult = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.EVENTS_DEALS_BANK_WITHDRAW.getTemplate(), 0, 0, 720, 1280, 90);

				if (bankDepositResult.isFound()) {
					emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, bankDepositResult.getPoint(), bankDepositResult.getPoint());
					emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(670, 40), new DTOPoint(670, 40), 15, 100);
				}

				// Verificar si el depósito está disponible
				DTOImageSearchResult isAvailableResult = emulatorManager.searchTemplate(EMULATOR_NUMBER, EnumTemplates.EVENTS_DEALS_BANK_DEPOSIT.getTemplate(), 0, 0, 720, 1280, 90);
				if (isAvailableResult.isFound()) {
					// Dependiendo del valor de bankDelay, seleccionar la opción de depósito
					if (bankDelay == 1) {
						emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(192, 712), new DTOPoint(228, 745));
					} else {
						emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(506, 701), new DTOPoint(571, 760));
					}

					sleepTask(2000);
					emulatorManager.executeSwipe(EMULATOR_NUMBER, new DTOPoint(168, 762), new DTOPoint(477, 760));
					emulatorManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(410, 877), new DTOPoint(589, 919));

					this.reschedule(LocalDateTime.now().plusDays(bankDelay));
					emulatorManager.tapBackButton(EMULATOR_NUMBER);
					return;
				}

				// OCR para calcular el tiempo restante
				try {
					String timeLeft = emulatorManager.ocrRegionText(EMULATOR_NUMBER, new DTOPoint(100, 585), new DTOPoint(290, 614));
					LocalDateTime nextBank = parseAndAddToNow(timeLeft);
					this.reschedule(nextBank);
					ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, nextBank);
					return;
				} catch (IOException | TesseractException e) {
					ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "Error al procesar OCR del tiempo restante");
					e.printStackTrace();
				}

				return;
			} else {
				// Si no se encuentra la pantalla de inicio, registrar advertencia y retroceder
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
				EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
				sleepTask(2000);
			}
			attempt++;
		}

		// Si después de 5 intentos no se encuentra el menú, se cancela la tarea
		if (attempt >= 5) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Menu not found, removing task from scheduler");
			this.setRecurring(false);
		}
	}

	public LocalDateTime parseAndAddToNow(String text) {
		if (text == null || text.trim().isEmpty()) {
			throw new IllegalArgumentException("El texto de entrada está vacío o es nulo.");
		}

		// Reemplazar 'O' (letra O mayúscula) por '0' (cero)
		text = text.toUpperCase().replace('O', '0').trim();

		// Remover espacios extras dentro de la cadena
		text = text.replaceAll("\\s+", " ");

		// Expresión regular para detectar formato con días o solo tiempo
		Pattern pattern = Pattern.compile("(\\d+)d\\s*(\\d{1,2}:\\d{2}:\\d{2})|^(\\d{1,2}:\\d{2}:\\d{2})$");
		Matcher matcher = pattern.matcher(text);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Formato inválido: '" + text + "'");
		}

		int daysToAdd = 0;
		String timeText;

		if (matcher.group(1) != null) { // Si hay un número de días
			daysToAdd = Integer.parseInt(matcher.group(1));
			timeText = matcher.group(2);
		} else { // Solo hay hora
			timeText = matcher.group(3);
		}

		// Parsear horas, minutos y segundos de la cadena de tiempo
		String[] timeParts = timeText.split(":");
		int hours = Integer.parseInt(timeParts[0]);
		int minutes = Integer.parseInt(timeParts[1]);
		int seconds = Integer.parseInt(timeParts[2]);

		// Obtener la fecha y hora actual
		LocalDateTime now = LocalDateTime.now();

		// Sumar días, horas, minutos y segundos
		return now.plusDays(daysToAdd).plusHours(hours).plusMinutes(minutes).plusSeconds(seconds);
	}

}
