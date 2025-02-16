package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import net.sourceforge.tess4j.TesseractException;

public class CrystalLaboratoryTask extends DelayedTask {

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	private final static String TASK_NAME = "Crystal Laboratory";

	public CrystalLaboratoryTask(DTOProfiles profile, TpDailyTaskEnum crystalLaboratory) {
		super(crystalLaboratory, LocalDateTime.now());
		this.profile = profile;
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "going to crystal laboratory");
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(1, 500), new DTOPoint(25, 590));
			sleepTask(2000);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(391, 618), new DTOPoint(417, 644));
			sleepTask(5000);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(530, 860), new DTOPoint(600, 1000));
			sleepTask(3000);

			String rem = null;
			try {
				rem = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(210, 1070), new DTOPoint(450, 1113));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (TesseractException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), rem);
			int total = parseRemainingToday(rem);
			if (total > 0) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "claiming crystals");
				for (int i = 0; i < total; i++) {
					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(230, 1135), new DTOPoint(390, 1200));
					sleepTask(1000);
				}
			}

			reschedule(UtilTime.getGameReset());
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "rescheduled task for tomorrow");
			ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.CRYSTAL_LABORATORY, UtilTime.getGameReset());
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, TASK_NAME, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}

	}

	/**
	 * Parsea un String con el formato "Remaining today: <número>" y retorna el número encontrado. Se consideran espacios extra y no distingue
	 * entre mayúsculas y minúsculas.
	 *
	 * @param input La cadena a parsear, por ejemplo: " Remaining today: 7 "
	 * @return El número extraído del String.
	 * @throws IllegalArgumentException si el formato del texto no es válido.
	 */
	public static int parseRemainingToday(String input) {
		// Compila la expresión regular con flag CASE_INSENSITIVE para ignorar mayúsculas/minúsculas.
		Pattern pattern = Pattern.compile("^\\s*remaining\\s*(today)?\\s*:\\s*(\\d+|—)\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(input);

		if (matcher.matches()) {
			String value = matcher.group(2);
			// Si el valor capturado es "—", retorna 0
			return "—".equals(value) ? 0 : Integer.parseInt(value);
		} else {
			throw new IllegalArgumentException("El formato del texto no es válido: " + input);
		}
	}

}
