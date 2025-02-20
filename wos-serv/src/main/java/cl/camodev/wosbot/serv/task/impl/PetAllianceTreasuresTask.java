package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;

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

public class PetAllianceTreasuresTask extends DelayedTask {

	private final DTOProfiles profile;

	private final String EMULATOR_NUMBER;

	private int attempts = 0;

	public PetAllianceTreasuresTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment) {
		super(heroRecruitment, LocalDateTime.now());
		this.profile = profile;
		this.EMULATOR_NUMBER = profile.getEmulatorNumber().toString();
	}

	@Override
	protected void execute() {
		if (attempts > 3) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "menu not found, removing task from scheduler");
			this.setRecurring(false);
			return;
		}

		DTOImageSearchResult homeResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_FURNACE.getTemplate(), 0, 0, 720, 1280, 90);
		DTOImageSearchResult worldResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_WORLD.getTemplate(), 0, 0, 720, 1280, 90);
		if (homeResult.isFound() || worldResult.isFound()) {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "going beast cage");

			DTOImageSearchResult petsResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_PETS.getTemplate(), 0, 0, 720, 1280, 90);
			if (petsResult.isFound()) {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "button pets found, taping");
				EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, petsResult.getPoint(), petsResult.getPoint());
				sleepTask(5000);

				DTOImageSearchResult beastCageResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_BEAST_CAGE.getTemplate(), 0, 0, 720, 1280, 90);
				if (beastCageResult.isFound()) {
					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, beastCageResult.getPoint(), beastCageResult.getPoint());
					sleepTask(3000);
					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(547, 1150), new DTOPoint(650, 1210));
					sleepTask(3000);

					EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(612, 1184), new DTOPoint(653, 1211));
					sleepTask(500);

					DTOImageSearchResult claimButton = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.PETS_BEAST_ALLIANCE_CLAIM.getTemplate(), 0, 0, 720, 1280, 90);
					if (claimButton.isFound()) {
						ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "claim button found, taping");
						EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, claimButton.getPoint(), claimButton.getPoint());
						ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.ALLIANCE_PET_TREASURE, UtilTime.getGameReset());
						this.reschedule(UtilTime.getGameReset());
						EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
						EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
						EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
					} else {
						EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, claimButton.getPoint(), claimButton.getPoint());
						ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Reward not found, rescheduling for reset");
						ServScheduler.getServices().updateDailyTaskStatus(profile, TpDailyTaskEnum.ALLIANCE_PET_TREASURE, UtilTime.getGameReset());
						this.reschedule(UtilTime.getGameReset());
						EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
						EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
						EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
					}

				} else {
					ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "beast cage not found retrying later");
					attempts++;

				}

			} else {
				ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "button pets not found retrying later");

			}

		} else {
			ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Home not found");
			EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);

		}
	}

}
