package cl.camodev.wosbot.web.api;

import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.web.api.dto.AllianceSettingsDto;
import cl.camodev.wosbot.web.service.AllianceSettingsService;
import cl.camodev.wosbot.web.service.ProfileNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for profile management operations.
 */
@RestController
@RequestMapping("/api")
public class ProfileApi {

    private static final Logger logger = LoggerFactory.getLogger(ProfileApi.class);
    private final AllianceSettingsService allianceSettingsService;

    public ProfileApi(AllianceSettingsService allianceSettingsService) {
        this.allianceSettingsService = allianceSettingsService;
    }

    @GetMapping("/profiles")
    public ResponseEntity<?> getProfiles() {
        try {
            List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();
            return ResponseEntity.ok(profiles);
        } catch (Exception e) {
            logger.error("Error fetching profiles: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to fetch profiles"));
        }
    }

    @GetMapping("/profiles/{profileId}/alliance")
    public ResponseEntity<?> getAllianceSettings(@PathVariable Long profileId) {
        try {
            AllianceSettingsDto dto = allianceSettingsService.getAllianceSettings(profileId);
            return ResponseEntity.ok(dto);
        } catch (ProfileNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Error loading alliance settings for profile {}: {}", profileId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to load alliance settings"));
        }
    }

    @PostMapping("/profiles/{profileId}/alliance")
    public ResponseEntity<?> updateAllianceSettings(@PathVariable Long profileId,
                                                    @RequestBody AllianceSettingsDto request) {
        try {
            AllianceSettingsDto updated = allianceSettingsService.updateAllianceSettings(profileId, request);
            return ResponseEntity.ok(updated);
        } catch (ProfileNotFoundException ex) {
            return ResponseEntity.status(404).body(Map.of("error", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Error updating alliance settings for profile {}: {}", profileId, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("error", "Failed to update alliance settings"));
        }
    }
}
