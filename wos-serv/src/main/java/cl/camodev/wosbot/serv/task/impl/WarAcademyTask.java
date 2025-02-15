package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cl.camodev.utiles.UtilTime;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.impl.ServScheduler;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class WarAcademyTask extends DelayedTask {
	private final DTOProfiles profile;
	private final String emulatorNumber;
	private static final String TASK_NAME = "War Academy";

	public WarAcademyTask(DTOProfiles profile, LocalDateTime scheduledTime) {
		super(TASK_NAME, scheduledTime);
		this.profile = profile;
		this.emulatorNumber = profile.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {
		DTODailyTaskStatus statusTask = ServScheduler.getServices().getDailyTaskStatus(profile, TpDailyTaskEnum.WAR_ACADEMY_SHARDS);

		if (statusTask.getFinished()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "This task is already done for today, rescheduling for reset");
			reschedule(UtilTime.getGameReset());
			return;
		}

		if (isHomeOrWorldScreenFound()) {
			performWarAcademyTask();
		} else {
			logAndExit("Home not found");
		}
	}

	private boolean isHomeOrWorldScreenFound() {
		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(emulatorNumber, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(emulatorNumber, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		return homeResult.isFound() || worldResult.isFound();
	}

	private void performWarAcademyTask() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "Going to war academy");

		navigateToWarAcademy();
		int totalShards = checkRemainingShards();

		if (totalShards > 0) {
			claimCrystalsShards();
		}

		ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.WAR_ACADEMY_SHARDS, true);
		EmulatorManager.getInstance().tapBackButton(emulatorNumber);
		reschedule(UtilTime.getGameReset());
	}

	private void navigateToWarAcademy() {
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(1, 500), new DTOPoint(25, 590));
		sleepTask(2000);
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(186, 533), new DTOPoint(265, 555));
		sleepTask(5000);
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(1, 787), new DTOPoint(38, 842));
		sleepTask(3000);
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(605, 139), new DTOPoint(622, 198));
	}

	private int checkRemainingShards() {
		String remainingText = EmulatorManager.getInstance().ocrRegionText(emulatorNumber, new DTOPoint(463, 452), new DTOPoint(624, 483));
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), remainingText);
		return parseRemaining(remainingText);
	}

	private void claimCrystalsShards() {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "Claiming crystals shards");
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(483, 500), new DTOPoint(605, 540));
		sleepTask(2000);
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(592, 685), new DTOPoint(629, 722));
		sleepTask(2000);
		EmulatorManager.getInstance().tapAtRandomPoint(emulatorNumber, new DTOPoint(241, 805), new DTOPoint(483, 857));
	}

	private void logAndExit(String message) {
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, TASK_NAME, profile.getName(), message);
		EmulatorManager.getInstance().tapBackButton(emulatorNumber);
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
		Pattern pattern = Pattern.compile("^\\s*remaining\\s*:\\s*(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(input);

		if (matcher.matches()) {
			return Integer.parseInt(matcher.group(1));
		} else {
			throw new IllegalArgumentException("El formato del texto no es válido: " + input);
		}
	}

	@Override
	public boolean isDailyTask() {
		return true;
	}
}
