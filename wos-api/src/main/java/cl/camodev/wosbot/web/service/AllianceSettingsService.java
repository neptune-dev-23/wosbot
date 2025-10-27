package cl.camodev.wosbot.web.service;

import cl.camodev.wosbot.console.enumerable.EnumConfigurationKey;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.IProfileDataChangeListener;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.web.api.dto.AllianceSettingsDto;
import cl.camodev.wosbot.web.api.dto.AllianceSettingsDto.AutojoinMode;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service that provides cached access to alliance automation settings and persists changes.
 */
@Service
public class AllianceSettingsService implements IProfileDataChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(AllianceSettingsService.class);
    private static final Duration CACHE_TTL = Duration.ofSeconds(10);

    private record CacheEntry(AllianceSettingsDto settings, Instant expiresAt) {}

    private final Map<Long, CacheEntry> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void registerListener() {
        ServProfiles.getServices().addProfileDataChangeListener(this);
    }

    public AllianceSettingsDto getAllianceSettings(long profileId) {
        AllianceSettingsDto cached = readFromCache(profileId);
        if (cached != null) {
            return cached;
        }
        AllianceSettingsDto refreshed = loadAndCache(profileId);
        logger.debug("Alliance settings cache refreshed for profile {}", profileId);
        return refreshed;
    }

    public AllianceSettingsDto updateAllianceSettings(long profileId, AllianceSettingsDto request) {
        Objects.requireNonNull(request, "Alliance settings request must not be null");

        DTOProfiles profile = ServProfiles.getServices().getProfileWithConfigs(profileId);
        if (profile == null) {
            throw new ProfileNotFoundException(profileId);
        }

        applySettings(profile, request);
        boolean saved = ServProfiles.getServices().saveProfile(profile);
        if (!saved) {
            throw new IllegalStateException("Failed to persist alliance settings for profile " + profileId);
        }

        AllianceSettingsDto updated = mapToDto(profile);
        cache.put(profileId, new CacheEntry(updated, Instant.now().plus(CACHE_TTL)));
        logger.debug("Alliance settings updated for profile {}", profileId);
        return updated;
    }

    @Override
    public void onProfileDataChanged(DTOProfiles profile) {
        if (profile == null || profile.getId() == null) {
            cache.clear();
            logger.debug("Alliance settings cache cleared due to unknown profile update");
            return;
        }
        cache.remove(profile.getId());
        logger.debug("Alliance settings cache invalidated for profile {}", profile.getId());
    }

    private AllianceSettingsDto readFromCache(long profileId) {
        CacheEntry entry = cache.get(profileId);
        if (entry == null) {
            return null;
        }
        if (Instant.now().isAfter(entry.expiresAt())) {
            cache.remove(profileId);
            return null;
        }
        return entry.settings();
    }

    private AllianceSettingsDto loadAndCache(long profileId) {
        DTOProfiles profile = ServProfiles.getServices().getProfileWithConfigs(profileId);
        if (profile == null) {
            throw new ProfileNotFoundException(profileId);
        }
        AllianceSettingsDto dto = mapToDto(profile);
        cache.put(profileId, new CacheEntry(dto, Instant.now().plus(CACHE_TTL)));
        return dto;
    }

    private AllianceSettingsDto mapToDto(DTOProfiles profile) {
        boolean techContribution = readBoolean(profile, EnumConfigurationKey.ALLIANCE_TECH_BOOL);
        int techOffset = readInt(profile, EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT);
        boolean chestClaim = readBoolean(profile, EnumConfigurationKey.ALLIANCE_CHESTS_BOOL);
        int chestOffset = readInt(profile, EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT);
        boolean honorChest = readBoolean(profile, EnumConfigurationKey.ALLIANCE_HONOR_CHEST_BOOL);
        boolean helpRequests = readBoolean(profile, EnumConfigurationKey.ALLIANCE_HELP_BOOL);
        boolean triumph = readBoolean(profile, EnumConfigurationKey.ALLIANCE_TRIUMPH_BOOL);
        int triumphOffset = readInt(profile, EnumConfigurationKey.ALLIANCE_TRIUMPH_OFFSET_INT);
        boolean lifeEssence = readBoolean(profile, EnumConfigurationKey.ALLIANCE_LIFE_ESSENCE_BOOL);
        int lifeEssenceOffset = readInt(profile, EnumConfigurationKey.ALLIANCE_LIFE_ESSENCE_OFFSET_INT);
        boolean autojoinEnabled = readBoolean(profile, EnumConfigurationKey.ALLIANCE_AUTOJOIN_BOOL);
        int autojoinQueues = readInt(profile, EnumConfigurationKey.ALLIANCE_AUTOJOIN_QUEUES_INT);
        boolean useAllTroops = readBoolean(profile, EnumConfigurationKey.ALLIANCE_AUTOJOIN_USE_ALL_TROOPS_BOOL);
        boolean useFormation = readBoolean(profile, EnumConfigurationKey.ALLIANCE_AUTOJOIN_USE_PREDEFINED_FORMATION_BOOL);

        AutojoinMode mode = useFormation ? AutojoinMode.USE_FORMATION : AutojoinMode.ALL_TROOPS;

        return new AllianceSettingsDto(
                techContribution,
                techOffset,
                chestClaim,
                chestOffset,
                honorChest,
                helpRequests,
                triumph,
                triumphOffset,
                lifeEssence,
                lifeEssenceOffset,
                autojoinEnabled,
                autojoinQueues,
                mode
        );
    }

    private void applySettings(DTOProfiles profile, AllianceSettingsDto request) {
        profile.setConfig(EnumConfigurationKey.ALLIANCE_TECH_BOOL, request.techContribution());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_TECH_OFFSET_INT, request.techOffsetMinutes());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_CHESTS_BOOL, request.chestClaim());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_CHESTS_OFFSET_INT, request.chestOffsetMinutes());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_HONOR_CHEST_BOOL, request.honorChest());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_HELP_BOOL, request.helpRequests());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_TRIUMPH_BOOL, request.triumph());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_TRIUMPH_OFFSET_INT, request.triumphOffsetMinutes());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_LIFE_ESSENCE_BOOL, request.lifeEssence());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_LIFE_ESSENCE_OFFSET_INT, request.lifeEssenceOffsetMinutes());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_AUTOJOIN_BOOL, request.autojoinEnabled());
        profile.setConfig(EnumConfigurationKey.ALLIANCE_AUTOJOIN_QUEUES_INT, request.autojoinQueues());

        boolean useFormation = request.autojoinMode() == AutojoinMode.USE_FORMATION;
        profile.setConfig(EnumConfigurationKey.ALLIANCE_AUTOJOIN_USE_ALL_TROOPS_BOOL, !useFormation);
        profile.setConfig(EnumConfigurationKey.ALLIANCE_AUTOJOIN_USE_PREDEFINED_FORMATION_BOOL, useFormation);
    }

    private boolean readBoolean(DTOProfiles profile, EnumConfigurationKey key) {
        try {
            Boolean value = profile.getConfig(key, Boolean.class);
            if (value != null) {
                return value;
            }
        } catch (Exception ex) {
            logger.warn("Failed to read boolean config {} for profile {}: {}", key.name(), profile.getId(), ex.getMessage());
        }
        return Boolean.parseBoolean(key.getDefaultValue());
    }

    private int readInt(DTOProfiles profile, EnumConfigurationKey key) {
        try {
            Integer value = profile.getConfig(key, Integer.class);
            if (value != null) {
                return value;
            }
        } catch (Exception ex) {
            logger.warn("Failed to read integer config {} for profile {}: {}", key.name(), profile.getId(), ex.getMessage());
        }
        try {
            return Integer.parseInt(key.getDefaultValue());
        } catch (NumberFormatException nfe) {
            logger.warn("Invalid integer default for {}: {}", key.name(), key.getDefaultValue());
            return 0;
        }
    }
}
