package cl.camodev.wosbot.serv.task.impl;

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
import net.sourceforge.tess4j.TesseractException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TrainingTroopsTask extends DelayedTask {

    private final TroopType troopType;

    public TrainingTroopsTask(DTOProfiles profile, TpDailyTaskEnum heroRecruitment, TroopType troopType) {
        super(profile, heroRecruitment);
        this.troopType = troopType;
    }

    public static LocalDateTime addTimeToLocalDateTime(LocalDateTime dateTime, String timeString) {
        // Regular expression to match the input format [n]d HH:mm:ss' o 'HH:mm:ss
        Pattern pattern = Pattern.compile("(?i).*?(?:(\\d+)\\s*d\\s*)?(\\d{1,2}:\\d{2}:\\d{2}).*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(timeString.trim());

        if (!matcher.matches()) {
            throw new IllegalArgumentException("Input does not match the expected format. Expected format: [n]d HH:mm:ss' o 'HH:mm:ss");
        }


        String daysStr = matcher.group(1);   // optional, can be null
        String timeStr = matcher.group(2);   // always present

        int daysToAdd = (daysStr != null) ? Integer.parseInt(daysStr) : 0;

        // parser for time part
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("H:mm:ss");
        LocalTime timePart = LocalTime.parse(timeStr, timeFormatter);


        return LocalDateTime.now()
                .plusDays(daysToAdd)
                .plusHours(timePart.getHour())
                .plusMinutes(timePart.getMinute())
                .plusSeconds(timePart.getSecond());
    }

    @Override
    protected void execute() {

        ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "going training " + troopType);
        EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(3, 513), new DTOPoint(26, 588));
        sleepTask(1000);
        EmulatorManager.getInstance().tapAtPoint(EMULATOR_NUMBER, new DTOPoint(110, 270));
        sleepTask(500);

        DTOImageSearchResult troopsResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, troopType.getTemplate(),  90);

        if (troopsResult.isFound()) {
            EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, troopsResult.getPoint(), troopsResult.getPoint());
            sleepTask(2000);

            EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(310, 650), new DTOPoint(450, 730), 15, 100);

            DTOImageSearchResult trainingResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.GAME_HOME_CAMP_TRAIN.getTemplate(),  90);
            DTOImageSearchResult upgrading = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.BUILDING_BUTTON_SPEED.getTemplate(),  90);


            if (trainingResult.isFound()) {
                EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, trainingResult.getPoint(), trainingResult.getPoint());
                sleepTask(500);

                EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, new DTOPoint(222, 157), new DTOPoint(504, 231), 10, 100);

                boolean promotionPriority = profile.getConfig(EnumConfigurationKey.TRAIN_PRIORITIZE_PROMOTION_BOOL, Boolean.class);

                if (promotionPriority) {
                  // i need to verify if there's troops available to promote, if not, i should train normally


                } else {
                  // i should train troops normally, so i need to search for the training button


                }


                DTOImageSearchResult trainingButtonResult = EmulatorManager.getInstance().searchTemplate(EMULATOR_NUMBER, EnumTemplates.TRAINING_TRAIN_BUTTON.getTemplate(),  90);


                if (trainingButtonResult.isFound() && !upgrading.isFound()) {
                    ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "Training available, taping");
                    EmulatorManager.getInstance().tapAtRandomPoint(EMULATOR_NUMBER, trainingButtonResult.getPoint(), trainingButtonResult.getPoint());
                    sleepTask(500);


                }

                ServLogs.getServices().appendLog(EnumTpMessageSeverity.INFO, taskName, profile.getName(), "getting next training schedule");
                Optional<LocalDateTime> optionalNextTime = extractNextTime();

                if (optionalNextTime.isPresent()) {
                    LocalDateTime nextSchedule = optionalNextTime.get();
                    this.reschedule(nextSchedule);
                    return;
                } else if (upgrading.isFound()) {

                    LocalDateTime fallback = LocalDateTime.now().plusHours(1);
                    logInfo("Upgrading in progress and no next time found, rescheduling in 1 hour");
                    this.reschedule(fallback);
                    return;
                }

                EmulatorManager.getInstance().tapBackButton(EMULATOR_NUMBER);
                sleepTask(500);
            }
        } else {
            ServLogs.getServices().appendLog(EnumTpMessageSeverity.WARNING, taskName, profile.getName(), "Troops not found");
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

    @Override
    protected Object getDistinctKey() {
        return troopType;
    }

    @Override
    public boolean provideDailyMissionProgress() {
        return true;
    }

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

}
