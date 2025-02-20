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
import cl.camodev.wosbot.serv.task.DelayedTask;

public class AllianceChestTask extends DelayedTask {

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	public AllianceChestTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment) {
		super(heroRecruitment, LocalDateTime.now());
		this.profile = profile;
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "going alliance chest");
			EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
			sleepTask(3000);

			DTOImageSearchResult allianceChestResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_CHEST_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);
			if (allianceChestResult.isFound()) {
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, allianceChestResult.getPoint(), allianceChestResult.getPoint());

				sleepTask(4000);

				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(56, 375), new DTOPoint(320, 420));

				sleepTask(2000);

				DTOImageSearchResult claimButton = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.ALLIANCE_CHEST_LOOT_CLAIM_BUTTON.getTemplate(), 0, 0, 720, 1280, 90);

				if (claimButton.isFound()) {
					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, claimButton.getPoint(), claimButton.getPoint(), 10, 300);
				}

				sleepTask(1000);

				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(410, 375), new DTOPoint(626, 420));

				sleepTask(2000);

				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(578, 1180), new DTOPoint(641, 1200), 10, 300);

				Integer offset = profile.getConfig(EnumConfigurationKey.INT_ALLIANCE_CHESTS_OFFSET, Integer.class);
				this.reschedule(LocalDateTime.now().plusHours(offset));
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "rescheduled for " + offset + " hours");

			} else {
				Integer offset = profile.getConfig(EnumConfigurationKey.INT_ALLIANCE_CHESTS_OFFSET, Integer.class);
				this.reschedule(LocalDateTime.now().plusHours(offset));
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "rescheduled for " + offset + " hours");

			}

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}
	}

}
