package cl.camodev.wosbot.serv.task.impl;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

import cl.camodev.utiles.number.NumberConverters;
import cl.camodev.utiles.number.NumberValidators;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTOImageSearchResult;
import cl.camodev.wosbot.ot.DTOPoint;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTOTesseractSettings;
import cl.camodev.wosbot.serv.task.DelayedTask;
import cl.camodev.wosbot.serv.task.TaskQueue;

import static cl.camodev.wosbot.console.enumerable.EnumConfigurationKey.ALLIANCE_SHOP_ENABLED_BOOL;

public class AllianceTechTask extends DelayedTask {

	public AllianceTechTask(DTOProfiles profile, TpDailyTaskEnum tpDailyTask) {
		super(profile, tpDailyTask);
	}

	@Override
	protected void execute() {

		logInfo("Starting alliance tech task.");

		// Go to the alliance tech section
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(493, 1187), new DTOPoint(561, 1240));
		sleepTask(3000);

		DTOImageSearchResult menuResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_TECH_BUTTON,  90);
		if (!menuResult.isFound()) {
			logWarning("Alliance tech button not found. Rescheduling.");
			this.reschedule(LocalDateTime.now()
					.plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT, Integer.class)));
			return;
		}

		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, menuResult.getPoint(), menuResult.getPoint());
		sleepTask(500);

		// search for thumb up button
		DTOImageSearchResult thumbUpResult = emuManager.searchTemplate(EMULATOR_NUMBER,
				EnumTemplates.ALLIANCE_TECH_THUMB_UP,  90);

		if (!thumbUpResult.isFound()) {
			logWarning("No task marked for upgrade. Rescheduling.");
			this.reschedule(LocalDateTime.now()
					.plusMinutes(profile.getConfig(EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT, Integer.class)));
			return;
		}

		logInfo("Thumb-up button found. Proceeding with donation.");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, thumbUpResult.getPoint(), thumbUpResult.getPoint());

		sleepTask(500);

		logInfo("Donating to alliance tech...");
		emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(450, 1000), new DTOPoint(580, 1050), 25, 150);
        if (profile.getConfig(ALLIANCE_SHOP_ENABLED_BOOL,Boolean.class)){
            emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(270,30), new DTOPoint(280,80),3,200);
            emuManager.tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(580,30), new DTOPoint(670,50),1,1000);


            Integer currentCoins = integerHelper.execute(
                    new DTOPoint(272,257),
                    new DTOPoint(443,285),
                    5,
                    200L,
                    DTOTesseractSettings.builder().setAllowedChars("0123456789").setPageSegMode(DTOTesseractSettings.PageSegMode.SINGLE_WORD).build(),
                    text -> NumberValidators.matchesPattern(text, Pattern.compile(".*?(\\d+).*")),
                    text -> NumberConverters.regexToInt(text, Pattern.compile(".*?(\\d+).*"))
            );

            if (currentCoins == null) {
                logWarning("Could not read current alliance coins.");
                return;
            }

            Integer minCoins = profile.getConfig(EnumConfigurationKey.ALLIANCE_SHOP_MIN_COINS_TO_ACTIVATE_INT, Integer.class);

            if (currentCoins > minCoins) {
                TaskQueue queue = servScheduler.getQueueManager().getQueue(profile.getId());
                if (queue!=null){
                    logInfo("Current alliance coins: " + currentCoins + ". Minimum required to activate shop: " + minCoins + ". Executing alliance shop task.");
                    queue.executeTaskNow(TpDailyTaskEnum.ALLIANCE_SHOP,true);

                }

            }
        }

		tapBackButton();
		tapBackButton();
		tapBackButton();

		Integer minutes = profile.getConfig(EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT, Integer.class);
		LocalDateTime nextSchedule = LocalDateTime.now()
				.plusMinutes(minutes);
		this.reschedule(nextSchedule);
		logInfo("Alliance tech task completed. Next execution scheduled in " + minutes + " minutes.");

	}

	@Override
	public boolean provideDailyMissionProgress() {return true;}

}
