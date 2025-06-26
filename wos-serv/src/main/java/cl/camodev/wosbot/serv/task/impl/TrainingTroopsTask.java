package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

public class TrainingTroopsTask extends DelayedTask {

	private final TroopType troopType;

	public enum TroopType {
		//@formatter:off
		INFANTRY(EnumTemplates.GAME_HOME_SHORTCUTS_INFANTRY), 
		LANCER(EnumTemplates.GAME_HOME_SHORTCUTS_LANCER), 
		MARKSMAN(EnumTemplates.GAME_HOME_SHORTCUTS_MARKSMAN);
		//@formatter:on

		private final EnumTemplates template;

		private TroopType(EnumTemplates template) {
			this.template = template;
		}

		public String getTemplate() {
			return template.getTemplate();
		}

	}

	public TrainingTroopsTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment, TroopType troopType) {
		super(profile, heroRecruitment);
		this.troopType = troopType;
	}

	@Override
	protected void execute() {

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {

			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "going training " + troopType);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
			sleepTask(1000);
			EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
			sleepTask(500);

			DTOImageSearchResult troopsResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, troopType.getTemplate(), 0, 0, 720, 1280, 90);

			if (troopsResult.isFound()) {
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, troopsResult.getPoint(), troopsResult.getPoint());
				sleepTask(2000);

				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(310, 650), new DTOPoint(450, 730), 15, 100);

				DTOImageSearchResult trainingResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_CAMP_TRAIN.getTemplate(), 0, 0, 720, 1280, 90);

				if (trainingResult.isFound()) {
					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, trainingResult.getPoint(), trainingResult.getPoint());
					sleepTask(500);

					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(222, 157), new DTOPoint(504, 231), 10, 100);

					DTOImageSearchResult trainingButtonResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.TRAINING_TRAIN_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);

					if (trainingButtonResult.isFound()) {
						ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Training available, taping");
						EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, trainingButtonResult.getPoint(), trainingButtonResult.getPoint());
						sleepTask(500);
					}

					ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "getting next training schedule");
					Optional<LocalDateTime> optionalNextTime = extractNextTime();

					if (optionalNextTime.isPresent()) {
						LocalDateTime nextSchedule = optionalNextTime.get();
						this.reschedule(nextSchedule);
						ServScheduler.getServices().updateDailyTaskStatus(profile, tpTask, nextSchedule);
					}

					EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
					sleepTask(500);
				}
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Troops not found");
			}

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}
	}

	private Optional<LocalDateTime> extractNextTime() {
		try {
			String text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(410, 997), new DTOPoint(586, 1048));
			return Optional.of(addTimeToLocalDateTime(LocalDateTime.now(), text));
		} catch (IOException | TesseractException e) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "Error processing OCR text");
			return Optional.empty();
		} catch (Exception e) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.ERROR, taskName, profile.getName(), "Unexpected error extracting time");
			return Optional.empty();
		}
	}

	public static LocalDateTime addTimeToLocalDateTime(LocalDateTime dateTime, String timeString) {
		// Expresión regular para validar formato hh:mm:ss o h:m:s sin caracteres extraños
		Pattern pattern = Pattern.compile("^\\s*(\\d{1,2})\\s*:\\s*(\\d{1,2})\\s*:\\s*(\\d{1,2})\\s*$");
		Matcher matcher = pattern.matcher(timeString);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("Formato incorrecto: " + timeString);
		}

		try {
			int hours = Integer.parseInt(matcher.group(1));
			int minutes = Integer.parseInt(matcher.group(2));
			int seconds = Integer.parseInt(matcher.group(3));

			// Validar rangos de tiempo
			if (hours < 0 || minutes < 0 || minutes >= 60 || seconds < 0 || seconds >= 60) {
				throw new IllegalArgumentException("Valores fuera de rango en: " + timeString);
			}

			Duration duration = Duration.ofHours(hours).plusMinutes(minutes).plusSeconds(seconds);

			return dateTime.plus(duration);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Error al procesar el tiempo: " + timeString, e);
		}
	}

}
