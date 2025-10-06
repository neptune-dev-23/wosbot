package cl.camodev.wosbot.serv.task.impl;

import cl.camodev.utiles.PriorityItemUtil;
import cl.camodev.utiles.time.TimeConverters;
import cl.camodev.utiles.time.TimeValidators;
import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.console.enumerable.EnumTemplates;
import cl.camodev.wosbot.console.enumerable.ExpertSkillItem;
import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.*;
import cl.camodev.wosbot.serv.task.DelayedTask;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * Task for training expert skills based on priority configuration
 * Handles training for Cyrille, Agnes, Holger, and Romulus skills
 */
public class ExpertSkillTrainingTask extends DelayedTask {

    public ExpertSkillTrainingTask(DTOProfiles profile, TpDailyTaskEnum tpTask) {
        super(profile, tpTask);
    }

    @Override
    protected void execute() {
        logInfo("Starting Expert Skill Training task.");

        //Get priority list configuration
        List<DTOPriorityItem> enabledPriorities = PriorityItemUtil.getEnabledPriorities(
                profile,
                EnumConfigurationKey.EXPERT_SKILL_TRAINING_PRIORITIES_STRING
        );

        if (enabledPriorities.isEmpty()) {
            logWarning("No enabled priorities found for expert skill training. Disabling task.");
            setRecurring(false);
            return;
        }

        // Navigate to experts screen
        tapRandomPoint(new DTOPoint(3, 513), new DTOPoint(26, 588)); //left menu

        int maxScrollAttempts = 5;
        DTOImageSearchResult trainingExpertButton = null;
        for (int attempt = 1; attempt <= maxScrollAttempts; attempt++) {
            swipe(new DTOPoint(255, 477), new DTOPoint(255, 400));
            sleepTask(500);
            trainingExpertButton = searchTemplateWithRetries(EnumTemplates.LEFT_MENU_EXPERT_TRAINING_BUTTON);
            if (trainingExpertButton.isFound()){
                break;
            }

        }
        if (!trainingExpertButton.isFound()){
            logInfo("No training expert found, ending task.");
            reschedule(LocalDateTime.now().plusMinutes(10));
            return;
        }
        tapPoint(trainingExpertButton.getPoint());
        sleepTask(2000);


        DTOImageSearchResult speedUpButton = searchTemplateWithRetries(EnumTemplates.EXPERT_TRAINING_SPEEDUP_ICON);
        if (speedUpButton.isFound()){
            //if im here means that there's a skill being trained, get training time and reschedule
            tapRandomPoint(speedUpButton.getPoint(),speedUpButton.getPoint(),1,500);
            Duration trainingTime = durationHelper.execute(
                    new DTOPoint(292,284),
                    new DTOPoint(432,314),
                    5,
                    300,
                    null,
                    TimeValidators::isHHmmss,
                    TimeConverters::hhmmssToDuration);
            if (trainingTime==null){
                return;
            }
            logInfo("A skill is currently being trained. Rescheduling task to run after training completes in " + trainingTime.toMinutes() + " minutes.");
            reschedule(LocalDateTime.now().plus(trainingTime));
            return;
        }
        //scroll down to normalize position
        for (int i = 0; i < 2; i++) {
            emuManager.executeSwipe(EMULATOR_NUMBER,new DTOPoint(358,1000),new DTOPoint(258,100));
        }

        //enter on cyrille
        tapRandomPoint( new DTOPoint(151,414),new DTOPoint(227,465),3,1000);

        // map the available experts to not over loop on experts that we don't have
        HashMap<EXPERTS, Boolean> expertAvailabilityMap = new HashMap<>();

        //search all expert badges to know which experts we have, we must search 1 by 1 then cling on change expert button (right arrow)

        EnumTemplates[] expertBadges = {
                EnumTemplates.EXPERT_TRAINING_CYRILLE_BADGE,
                EnumTemplates.EXPERT_TRAINING_AGNES_BADGE,
                EnumTemplates.EXPERT_TRAINING_HOLGER_BADGE,
                EnumTemplates.EXPERT_TRAINING_ROMULUS_BADGE
        };

        for (int i = 0; i < 10; i++) {
            Optional<BadgeSearchResult> foundResult = Arrays.stream(expertBadges)
                    .parallel()
                    .map(expertBadge -> {
                        DTOImageSearchResult badge = searchTemplateWithRetries(expertBadge);
                        EXPERTS expert = getExpertFromTemplate(expertBadge);
                        return new BadgeSearchResult(badge, expert, expertBadge);
                    })
                    .filter(result -> result.badge.isFound())
                    .findAny();

            if (foundResult.isPresent()) {
                BadgeSearchResult result = foundResult.get();
                boolean alreadyAvailable = expertAvailabilityMap.getOrDefault(result.expert, false);

                if (alreadyAvailable) {
                    break;
                }
                expertAvailabilityMap.put(result.expert, true);
            } else {
                Arrays.stream(expertBadges)
                        .map(this::getExpertFromTemplate)
                        .forEach(expert -> expertAvailabilityMap.putIfAbsent(expert, false));
            }
            tapPoint(new DTOPoint(671, 650)); // right arrow to change expert
            sleepTask(300);
        }

        long availableCount = expertAvailabilityMap.values().stream().filter(Boolean::booleanValue).count();
        if (availableCount == 0) {
            logInfo("No experts found, scheduling to check again in 10 minutes.");
            reschedule(LocalDateTime.now().plusMinutes(10));
            return;
        }

        //print expert availability map
        for (EnumTemplates expertBadge : expertBadges) {
            EXPERTS expert = getExpertFromTemplate(expertBadge);
            if (expertAvailabilityMap.getOrDefault(expert, false)) {
                logInfo(expert + " is available.");
            } else {
                logInfo(expert + " is not available. Related skills will be skipped.");
            }
        }

        // switch to skills tab
        tapRandomPoint(new DTOPoint(500,1232), new DTOPoint(570,1251),1,500);

        // 3. Iterate through priorities and train skills
        for (DTOPriorityItem priorityItem : enabledPriorities) {
            //check if the expert related is available
            ExpertSkillItem skillItem = ExpertSkillItem.valueOf(priorityItem.getIdentifier().toUpperCase());
            EXPERTS expert = getExpertFromEnum(skillItem);

            if (!expertAvailabilityMap.getOrDefault(expert, false)) {
                logInfo("Skipping " + priorityItem.getName() + " because " + expert + " is not available.");
                continue;
            }
            logInfo("Training skill: " + priorityItem.getName() + " for expert: " + expert);
            //navigate to expert via template
            EnumTemplates expertBadgeTemplate = getExpertTemplate(skillItem);
            DTOImageSearchResult expertBadgeResult = searchTemplateWithRetries(expertBadgeTemplate, 90, 2);
            int availableExperts = (int) expertAvailabilityMap.values().stream().filter(Boolean::booleanValue).count();
            int currentIndex = 0;
            while (!expertBadgeResult.isFound() && currentIndex < availableExperts) {
                tapPoint(new DTOPoint(671,650));
                expertBadgeResult = searchTemplateWithRetries(expertBadgeTemplate, 90, 2);
                currentIndex++;
            }
            if (!expertBadgeResult.isFound()) {
                logWarning("Could not navigate to expert: " + expert + ". Skipping skill: " + priorityItem.getName());
                continue;
            }
            DTOArea skillArea = getSkillArea(skillItem);
            tapRandomPoint(skillArea.topLeft(), skillArea.bottomRight(),1,300);

            //check if skill is maxed or locked
            DTOImageSearchResult learnResult = searchTemplateWithRetries(EnumTemplates.EXPERT_TRAINING_LEARN_BUTTON, 90, 3);

            if (!learnResult.isFound()) {
                logInfo("Skill " + priorityItem.getName() + " is either maxed or locked. Skipping.");
                continue;
            }

            tapPoint(learnResult.getPoint());
            sleepTask(500);

            // check if the skill have pending points to learn, lets search the learn button again
            learnResult = searchTemplateWithRetries(EnumTemplates.EXPERT_TRAINING_LEARN_BUTTON, 90, 3);
            if (learnResult.isFound()) {
               logInfo("Skill " + priorityItem.getName() + " has no available skill points to learn. Skipping.");
               tapRandomPoint(new DTOPoint(360,33), new DTOPoint(374,44),3,300);
                continue;
            }

            // we must try with the times in decreasing order to not waste time 23h -> 10h -> 2h -> 10m (if 10 also fails, use it as last resort)
            List<LearningTime> timesDescending = List.of(
                    LearningTime.TIME_23_00_00,
                    LearningTime.TIME_10_00_00,
                    LearningTime.TIME_02_00_00,
                    LearningTime.TIME_00_10_00
            );

            for (LearningTime learningTime : timesDescending) {
                DTOArea timeCheckboxArea = getLearningTimeCheckbox(learningTime);
                tapRandomPoint(timeCheckboxArea.topLeft(), timeCheckboxArea.bottomRight(),1,300);
                tapRandomPoint(new DTOPoint(474,888), new DTOPoint(579,910),1,400);
                //check if the pop-up disappeared, that mean it was successful

                DTOImageSearchResult badgeResult = searchTemplateWithRetries(expertBadgeTemplate, 90, 3);

                if (badgeResult.isFound()) {
                    logInfo("Successfully started training for skill: " + priorityItem.getName() + " with duration: " + learningTime.label());
                    this.reschedule(LocalDateTime.now().plus(learningTime.duration())); // add 1 minute buffer
                    return;
                }else{
                    tapRandomPoint(new DTOPoint(284,329), new DTOPoint(452,359),1,300);
                    if (learningTime.equals(LearningTime.TIME_00_10_00)) {
                        logInfo("All time options failed, but skill training did not start. Forcing 10 minutes option as last resort.");
                        tapRandomPoint(timeCheckboxArea.topLeft(), timeCheckboxArea.bottomRight(),1,400);
                        tapRandomPoint(new DTOPoint(454,777), new DTOPoint(573,800),1,400);
                        return;
                    }
                }
            }
        }
    }

    private EXPERTS getExpertFromEnum(ExpertSkillItem expertSkillItem) {
        return switch (expertSkillItem) {
            case AGNES_SKILL_1, AGNES_SKILL_2, AGNES_SKILL_3, AGNES_SKILL_4 -> EXPERTS.AGNES;
            case CYRILLE_SKILL_1, CYRILLE_SKILL_2, CYRILLE_SKILL_3, CYRILLE_SKILL_4 -> EXPERTS.CYRILLE;
            case HOLGER_SKILL_1, HOLGER_SKILL_2, HOLGER_SKILL_3, HOLGER_SKILL_4 -> EXPERTS.HOLGER;
            case ROMULUS_SKILL_1, ROMULUS_SKILL_2, ROMULUS_SKILL_3, ROMULUS_SKILL_4 -> EXPERTS.ROMULUS;
        };
    }

    public EnumTemplates getExpertTemplate(ExpertSkillItem expertSkillItem) {

        return switch (expertSkillItem) {
            case AGNES_SKILL_1, AGNES_SKILL_2, AGNES_SKILL_3, AGNES_SKILL_4 ->
                    EnumTemplates.EXPERT_TRAINING_AGNES_BADGE;
            case CYRILLE_SKILL_1, CYRILLE_SKILL_2, CYRILLE_SKILL_3, CYRILLE_SKILL_4 ->
                    EnumTemplates.EXPERT_TRAINING_CYRILLE_BADGE;
            case HOLGER_SKILL_1, HOLGER_SKILL_2, HOLGER_SKILL_3, HOLGER_SKILL_4 ->
                    EnumTemplates.EXPERT_TRAINING_HOLGER_BADGE;
            case ROMULUS_SKILL_1, ROMULUS_SKILL_2, ROMULUS_SKILL_3, ROMULUS_SKILL_4 ->
                    EnumTemplates.EXPERT_TRAINING_ROMULUS_BADGE;
        };

    }

private DTOArea getSkillArea(ExpertSkillItem skillItem) {
    return switch (skillItem) {
        case CYRILLE_SKILL_1, AGNES_SKILL_1, HOLGER_SKILL_1, ROMULUS_SKILL_1 ->
                new DTOArea(new DTOPoint(62, 1032), new DTOPoint(132, 1102));
        case CYRILLE_SKILL_2, AGNES_SKILL_2, HOLGER_SKILL_2, ROMULUS_SKILL_2 ->
                new DTOArea(new DTOPoint(237, 1032), new DTOPoint(307, 1102));
        case CYRILLE_SKILL_3, AGNES_SKILL_3, HOLGER_SKILL_3, ROMULUS_SKILL_3 ->
                new DTOArea(new DTOPoint(412, 1032), new DTOPoint(482, 1102));
        case CYRILLE_SKILL_4, AGNES_SKILL_4, HOLGER_SKILL_4, ROMULUS_SKILL_4 ->
                new DTOArea(new DTOPoint(587, 1032), new DTOPoint(657, 1102));
    };
}

    private EXPERTS getExpertFromTemplate(EnumTemplates template) {

        return switch (template) {
            case EXPERT_TRAINING_AGNES_BADGE -> EXPERTS.AGNES;
            case EXPERT_TRAINING_CYRILLE_BADGE -> EXPERTS.CYRILLE;
            case EXPERT_TRAINING_HOLGER_BADGE -> EXPERTS.HOLGER;
            case EXPERT_TRAINING_ROMULUS_BADGE -> EXPERTS.ROMULUS;
            default -> null;
        };
    }

    public record LearningTime(String label, Duration duration) {

        public static final LearningTime TIME_00_10_00 = new LearningTime("00:10:00", Duration.ofMinutes(10));
        public static final LearningTime TIME_02_00_00 = new LearningTime("02:00:00", Duration.ofHours(2));
        public static final LearningTime TIME_10_00_00 = new LearningTime("10:00:00", Duration.ofHours(10));
        public static final LearningTime TIME_23_00_00 = new LearningTime("23:00:00", Duration.ofHours(23));

    }

    private DTOArea getLearningTimeCheckbox(LearningTime time) {
        if (time.equals(LearningTime.TIME_00_10_00)) {
            return new DTOArea(new DTOPoint(90, 686), new DTOPoint(114, 711));
        } else if (time.equals(LearningTime.TIME_02_00_00)) {
            return new DTOArea(new DTOPoint(373, 688), new DTOPoint(402, 713));
        } else if (time.equals(LearningTime.TIME_10_00_00)) {
            return new DTOArea(new DTOPoint(90, 766), new DTOPoint(116, 792));
        } else if (time.equals(LearningTime.TIME_23_00_00)) {
            return new DTOArea(new DTOPoint(373, 765), new DTOPoint(403, 792));
        }
        throw new IllegalArgumentException("Unknown learning time: " + time);
    }

    private enum EXPERTS {
        CYRILLE,
        AGNES,
        HOLGER,
        ROMULUS
    }
    record BadgeSearchResult(DTOImageSearchResult badge, EXPERTS expert, EnumTemplates template) {}


}
