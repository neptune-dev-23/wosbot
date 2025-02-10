package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class WarAcademyTask extends DelayedTask {

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	private final static String TASK_NAME = "War Academy";

	public WarAcademyTask(DTOProfiles profile, LocalDateTime scheduledTime) {
		super(TASK_NAME, scheduledTime);
		this.profile = profile;
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {

//		String text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(40, 770), new DTOPoint(350, 810));
//		System.out.println(text);

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "going to war academy");
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1, 500), new DTOPoint(25, 590));
			sleepTask(2000);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(186, 533), new DTOPoint(265, 555));
			sleepTask(5000);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1, 787), new DTOPoint(38, 842));
			sleepTask(3000);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(605, 139), new DTOPoint(622, 198));

			String rem = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(463, 452), new DTOPoint(624, 483));
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), rem);
			int total = parseRemaining(rem);
			if (total > 0) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "claiming crystals shards");
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(483, 500), new DTOPoint(605, 540));
				sleepTask(2000);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(592, 685), new DTOPoint(629, 722));
				sleepTask(2000);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(241, 805), new DTOPoint(483, 857));
			}

			ZonedDateTime nowUtc = ZonedDateTime.now(ZoneId.of("UTC"));
			ZonedDateTime nextUtcMidnight = nowUtc.toLocalDate().plusDays(1).atStartOfDay(ZoneId.of("UTC"));
			ZonedDateTime localNextMidnight = nextUtcMidnight.withZoneSameInstant(ZoneId.systemDefault());
			reschedule(localNextMidnight.toLocalDateTime());
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "rescheduled task for tomorrow");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, TASK_NAME, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}

	}

	/**
	 * Parsea un String con el formato "Remaining : <número>" y retorna el número encontrado. Se consideran espacios extra y no distingue entre
	 * mayúsculas y minúsculas.
	 *
	 * @param input La cadena a parsear, por ejemplo: " Remaining today: 7 "
	 * @return El número extraído del String.
	 * @throws IllegalArgumentException si el formato del texto no es válido.
	 */
	public static int parseRemaining(String input) {
		// Expresión regular que permite espacios antes y después de "Remaining" y ":"
		Pattern pattern = Pattern.compile("^\\s*remaining\\s*:\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(input);

		if (matcher.matches()) {
			// Extrae el número y lo convierte en entero
			return Integer.parseInt(matcher.group(1));
		} else {
			throw new IllegalArgumentException("El formato del texto no es válido: " + input);
		}
	}

}
