package cl.camodev.wosbot.web.api.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * DTO that exposes alliance-related automation settings for a profile.
 * The structure intentionally mirrors the fields required by the web UI.
 */
public record AllianceSettingsDto(
        boolean techContribution,
        int techOffsetMinutes,
        boolean chestClaim,
        int chestOffsetMinutes,
        boolean honorChest,
        boolean helpRequests,
        boolean triumph,
        int triumphOffsetMinutes,
        boolean lifeEssence,
        int lifeEssenceOffsetMinutes,
        boolean autojoinEnabled,
        int autojoinQueues,
        AutojoinMode autojoinMode
) {

    public AllianceSettingsDto {
        techOffsetMinutes = sanitizeOffset(techOffsetMinutes);
        chestOffsetMinutes = sanitizeOffset(chestOffsetMinutes);
        triumphOffsetMinutes = sanitizeOffset(triumphOffsetMinutes);
        lifeEssenceOffsetMinutes = sanitizeOffset(lifeEssenceOffsetMinutes);
        autojoinQueues = Math.max(1, autojoinQueues);
        if (autojoinMode == null) {
            autojoinMode = AutojoinMode.ALL_TROOPS;
        }
    }

    private static int sanitizeOffset(int minutes) {
        return Math.max(0, minutes);
    }

    public enum AutojoinMode {
        ALL_TROOPS("allTroops"),
        USE_FORMATION("useFormation");

        private final String wireValue;

        AutojoinMode(String wireValue) {
            this.wireValue = wireValue;
        }

        @JsonValue
        public String getWireValue() {
            return wireValue;
        }

        @JsonCreator
        public static AutojoinMode fromWireValue(String wireValue) {
            if (wireValue == null || wireValue.isBlank()) {
                return ALL_TROOPS;
            }
            for (AutojoinMode mode : values()) {
                if (mode.wireValue.equalsIgnoreCase(wireValue)) {
                    return mode;
                }
            }
            return ALL_TROOPS;
        }
    }
}
