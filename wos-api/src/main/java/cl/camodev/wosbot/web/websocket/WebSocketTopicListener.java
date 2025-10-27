package cl.camodev.wosbot.web.websocket;

import org.springframework.web.socket.WebSocketSession;

/**
 * Contract for services that want to react when a client subscribes to a topic.
 */
public interface WebSocketTopicListener {

    /**
     * @return the topic identifier (e.g. "logs") this listener reacts to.
     */
    String topic();

    /**
     * Called after a client subscribes to the topic so the listener can push an initial snapshot.
     *
     * @param session the session that subscribed
     */
    void onSubscribe(WebSocketSession session);
}
