package cl.camodev.wosbot.web.streaming;

import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.serv.impl.ServTaskManager;
import cl.camodev.wosbot.taskmanager.ITaskStatusChangeListener;
import cl.camodev.wosbot.web.task.TaskDataService;
import cl.camodev.wosbot.web.websocket.WebSocketMessagingService;
import cl.camodev.wosbot.web.websocket.WebSocketTopicListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;

import java.util.List;
import java.util.Map;

/**
 * Broadcasts task state updates over WebSocket and delivers snapshots on subscription.
 */
@Service
public class TaskStateStreamingService implements ITaskStatusChangeListener, WebSocketTopicListener {

    private static final Logger logger = LoggerFactory.getLogger(TaskStateStreamingService.class);

    private final WebSocketMessagingService messagingService;
    private final TaskDataService taskDataService;

    public TaskStateStreamingService(WebSocketMessagingService messagingService, TaskDataService taskDataService) {
        this.messagingService = messagingService;
        this.taskDataService = taskDataService;
    }

    @PostConstruct
    public void registerListener() {
        ServTaskManager.getInstance().addTaskStatusChangeListener(this);
        logger.debug("Task state streaming service registered as listener");
    }

    @Override
    public String topic() {
        return "tasks";
    }

    @Override
    public void onSubscribe(WebSocketSession session) {
        Map<Long, List<DTOTaskState>> snapshot = taskDataService.getTasksByProfile();
        messagingService.send(session, "tasks.snapshot", snapshot);
    }

    @Override
    public void onTaskStatusChange(Long profileId, int taskNameId, DTOTaskState taskState) {
        if (taskState == null) {
            return;
        }
        taskState.setProfileId(profileId);
        taskState.setTaskId(taskNameId);
        taskDataService.decorateTaskName(taskState);
        logger.debug("Task update profile={} task={} executing={} scheduled={}",
                profileId, taskState.getTaskName(), taskState.isExecuting(), taskState.isScheduled());

        TaskUpdatePayload payload = new TaskUpdatePayload(profileId, taskState);
        messagingService.broadcast("tasks", "tasks.update", payload);
    }

    private record TaskUpdatePayload(Long profileId, DTOTaskState task) {
    }
}
