package cl.camodev.wosbot.web.streaming;

import cl.camodev.wosbot.ot.DTOProfileStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.serv.IProfileDataChangeListener;
import cl.camodev.wosbot.serv.IProfileStatusChangeListener;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.web.websocket.WebSocketMessagingService;
import cl.camodev.wosbot.web.websocket.WebSocketTopicListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collections;
import java.util.List;

/**
 * Streams profile data over WebSocket so clients can cache a single snapshot and receive real-time updates.
 */
@Service
public class ProfileStreamingService implements IProfileStatusChangeListener, IProfileDataChangeListener, WebSocketTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(ProfileStreamingService.class);

    private final WebSocketMessagingService messagingService;

    private record ProfileStatusUpdate(Long id, String status) {}

    private record ProfileUpdatePayload(List<DTOProfiles> changed, List<Long> removed, List<ProfileStatusUpdate> statuses) {}

    public ProfileStreamingService(WebSocketMessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @PostConstruct
    public void registerListeners() {
        ServProfiles.getServices().addProfileStatusChangeListerner(this);
        ServProfiles.getServices().addProfileDataChangeListener(this);
        logger.info("Profile streaming service registered as listener");
    }

    @Override
    public String topic() {
        return "profiles";
    }

    @Override
    public void onSubscribe(WebSocketSession session) {
        sendSnapshot(session);
    }

    @Override
    public void onProfileStatusChange(DTOProfileStatus status) {
        logger.debug("Profile status change received for profile {}", status.getId());
        if (status == null || status.getId() == null) {
            return;
        }

        DTOProfiles latest = ServProfiles.getServices().getProfileWithConfigs(status.getId());
        ProfileUpdatePayload payload = new ProfileUpdatePayload(
                latest != null ? Collections.singletonList(latest) : null,
                null,
                Collections.singletonList(new ProfileStatusUpdate(status.getId(), status.getStatus()))
        );
        broadcastUpdate(payload);
    }

    @Override
    public void onProfileDataChanged(DTOProfiles profile) {
        logger.debug("Profile data change detected for profile {}", profile != null ? profile.getId() : "unknown");
        if (profile == null || profile.getId() == null) {
            broadcastSnapshot();
            return;
        }

        DTOProfiles latest = ServProfiles.getServices().getProfileWithConfigs(profile.getId());
        if (latest != null) {
            ProfileUpdatePayload payload = new ProfileUpdatePayload(
                    Collections.singletonList(latest),
                    null,
                    null
            );
            broadcastUpdate(payload);
        } else {
            ProfileUpdatePayload payload = new ProfileUpdatePayload(
                    null,
                    Collections.singletonList(profile.getId()),
                    null
            );
            broadcastUpdate(payload);
        }
    }

    private void broadcastSnapshot() {
        List<DTOProfiles> snapshot = loadSnapshot();
        messagingService.broadcast("profiles", "profiles.update", snapshot);
    }

    private void sendSnapshot(WebSocketSession session) {
        List<DTOProfiles> snapshot = loadSnapshot();
        messagingService.send(session, "profiles.snapshot", snapshot);
    }

    private List<DTOProfiles> loadSnapshot() {
        try {
            return ServProfiles.getServices().getProfiles();
        } catch (Exception ex) {
            logger.error("Failed to load profiles snapshot: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    private void broadcastUpdate(ProfileUpdatePayload payload) {
        if (payload == null) {
            return;
        }
        messagingService.broadcast("profiles", "profiles.update", payload);
    }
}
