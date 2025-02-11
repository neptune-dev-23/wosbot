package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

import cl.camodev.wosbot.console.enumerable.EnumTpMessageSeverity;
import cl.camodev.wosbot.emulator.EmulatorManager;
import cl.camodev.wosbot.ex.StopExecutionException;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServLogs;
import cl.camodev.wosbot.serv.task.DelayedTask;

public class InitializeTask extends DelayedTask {
	boolean isStarted = false;

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	private final static String TASK_NAME = "Initialize";

	public InitializeTask(DTOProfiles profile, LocalDateTime scheduledTime) {
		super(TASK_NAME, scheduledTime);
		this.profile = profile;
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {
		this.setRecurring(false);
		ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "Checking emulator status");
		while (!isStarted) {

			boolean playerState = EmulatorManager.getInstance().getPlayerState(EMULATOR_NUMBER);

			if (playerState) {
				isStarted = true;
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "emulator found");
			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "emulator not found");
				EmulatorManager.getInstance().launchPlayer(EMULATOR_NUMBER);
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "waiting 15 seconds before checking again");
				try {
					Thread.sleep(15000);
				} catch (Exception e) {

				}
			}

		}

		if (!EmulatorManager.getInstance().isWhiteoutSurvivalInstalled(EMULATOR_NUMBER)) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "whiteout survival not installed, stopping queue");
			throw new StopExecutionException("Game not installed");
		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, TASK_NAME, profile.getName(), "whiteout survival installed");
			EmulatorManager.getInstance().connectADB(profile.getEmulatorNumber().toString());
			EmulatorManager.getInstance().launchGame(EMULATOR_NUMBER);
			sleepTask(15000);
			this.setRecurring(false);

		}
	}

	public String getEmulatorNumber() {
		return EMULATOR_NUMBER;
	}
}