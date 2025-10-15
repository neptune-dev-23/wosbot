package cl.camodev.wosbot.web.service;

/**
 * Exception thrown when a profile cannot be located in persistence.
 */
public class ProfileNotFoundException extends RuntimeException {
    public ProfileNotFoundException(long profileId) {
        super("Profile " + profileId + " not found");
    }
}
