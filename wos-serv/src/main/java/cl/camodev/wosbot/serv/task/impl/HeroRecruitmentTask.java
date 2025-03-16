package cl.camodev.wosbot.serv.task.impl;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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

public class HeroRecruitmentTask extends DelayedTask {

	public HeroRecruitmentTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {

//		String text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(40, 770), new DTOPoint(350, 810));
//		System.out.println(text);

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "going hero recruitment");
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(160, 1190), new DTOPoint(217, 1250));
			sleepTask(3000);
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(400, 1190), new DTOPoint(660, 1250));
			sleepTask(3000);

			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "evaluating advanced recruitment");
			DTOImageSearchResult claimResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.HERO_RECRUIT_CLAIM.getTemplate(), 40, 800, 300, 95, 95);
			LocalDateTime nextAdvanced = null;
			if (claimResult.isFound()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "advanced recruitment available, tapping");
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 827), new DTOPoint(315, 875));
				sleepTask(3000);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(3000);
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "getting next recruitment time");
				String text = "";
				try {
					text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(40, 770), new DTOPoint(350, 810));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TesseractException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), text + " rescheduling task");
				nextAdvanced = parseNextFree(text);
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "no rewards to claim, getting next recruitment time");
				String text = "";
				try {
					text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(40, 770), new DTOPoint(350, 810));
				} catch (IOException | TesseractException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				nextAdvanced = parseNextFree(text);
			}

			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "evaluating epic recruitment");
			DTOImageSearchResult claimResultEpic = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.HERO_RECRUIT_CLAIM.getTemplate(), 40, 1160, 300, 95, 95);
			LocalDateTime nextEpic;
			if (claimResultEpic.isFound()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "epic recruitment available, tapping");
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(70, 1180), new DTOPoint(315, 1230));
				sleepTask(3000);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(300);
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(80, 90), new DTOPoint(140, 130));
				sleepTask(3000);
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "getting next recruitment time");
				String text = "";
				try {
					text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(53, 1130), new DTOPoint(330, 1160));
				} catch (IOException | TesseractException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				nextEpic = parseNextFree(text);
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "no rewards to claim, getting next recruitment time");
				String text = "";
				try {
					text = EmulatorManager.getInstance().ocrRegionText(EMULATOR_NUMBER, new DTOPoint(53, 1130), new DTOPoint(330, 1160));
				} catch (IOException | TesseractException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				nextEpic = parseNextFree(text);
			}

			LocalDateTime nextExecution = getEarliest(nextAdvanced, nextEpic);
			this.reschedule(nextExecution);
			ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.HERO_RECRUITMENT, nextExecution);
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}

	}

	public static LocalDateTime getEarliest(LocalDateTime dt1, LocalDateTime dt2) {
		return dt1.isBefore(dt2) ? dt1 : dt2;
	}

	public static LocalDateTime parseNextFree(String input) {
		// Expresión regular para capturar opcionalmente los días y obligatoriamente la hora
		Pattern pattern = Pattern.compile("^(?:~\\s*)?Next free:\\s*(?:(\\d+)d\\s+)?(\\d{1,2}:\\d{2}:\\d{2})\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(input);

		if (!matcher.matches()) {
			throw new IllegalArgumentException("El formato del texto no es válido: " + input);
		}

		// Grupo 1: días (opcional)
		String daysStr = matcher.group(1);
		// Grupo 2: la hora en formato HH:mm:ss
		String timeStr = matcher.group(2);

		// Convertir el número de días (si está presente) o 0 en caso contrario
		int daysToAdd = daysStr != null ? Integer.parseInt(daysStr) : 0;

		// Parsear la parte de la hora utilizando un formateador que permita 1 o 2 dígitos para la hora
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
		LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);

		// Obtener la fecha y hora actuales
		LocalDateTime now = LocalDateTime.now();

		// Sumar al 'now' los días, horas, minutos y segundos obtenidos
		LocalDateTime result = now.plusDays(daysToAdd).plusHours(timePart.getHour()).plusMinutes(timePart.getMinute()).plusSeconds(timePart.getSecond());

		return result;
	}

}
