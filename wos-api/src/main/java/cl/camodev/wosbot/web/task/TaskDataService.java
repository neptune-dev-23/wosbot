package cl.camodev.wosbot.web.task;

import cl.camodev.wosbot.console.enumerable.TpDailyTaskEnum;
import cl.camodev.wosbot.ot.DTODailyTaskStatus;
import cl.camodev.wosbot.ot.DTOProfiles;
import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.serv.impl.ServProfiles;
import cl.camodev.wosbot.serv.impl.ServTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Provides aggregated task data for profiles, shared by REST and WebSocket layers.
 */
@Service
public class TaskDataService {

    private static final Logger logger = LoggerFactory.getLogger(TaskDataService.class);

    public Map<Long, List<DTOTaskState>> getTasksByProfile() {
        Map<Long, List<DTOTaskState>> tasksMap = new HashMap<>();
        List<DTOProfiles> profiles = ServProfiles.getServices().getProfiles();

        for (DTOProfiles profile : profiles) {
            List<DTODailyTaskStatus> taskStatuses =
                    ServTaskManager.getInstance().getDailyTaskStatusPersistence(profile.getId());

            if (taskStatuses.isEmpty()) {
                continue;
            }

            List<DTOTaskState> taskStates = new ArrayList<>();
            for (DTODailyTaskStatus taskStatus : taskStatuses) {
                DTOTaskState taskState = ServTaskManager.getInstance()
                        .getTaskState(profile.getId(), taskStatus.getIdTpDailyTask());

                if (taskState == null) {
                    taskState = new DTOTaskState();
                    taskState.setProfileId(profile.getId());
                    taskState.setTaskId(taskStatus.getIdTpDailyTask());
                    taskState.setScheduled(false);
                    taskState.setExecuting(false);
                    taskState.setLastExecutionTime(taskStatus.getLastExecution());
                    taskState.setNextExecutionTime(taskStatus.getNextSchedule());
                }

                decorateTaskName(taskState);
                taskStates.add(taskState);
            }

            if (!taskStates.isEmpty()) {
                tasksMap.put(profile.getId(), taskStates);
            }
        }

        logger.debug("Aggregated tasks for {} profiles", tasksMap.size());
        return tasksMap;
    }

    public void rescheduleTask(DTOTaskState task) {
        if (task == null || task.getProfileId() == null || task.getTaskId() == null) {
            throw new IllegalArgumentException("Task, profile ID, and task ID must not be null");
        }
        if (task.getNextExecutionTime() == null) {
            throw new IllegalArgumentException("Next execution time must not be null");
        }

        String schedule = task.getNextExecutionTime().toString();
        ServTaskManager.getInstance().rescheduleTask(task.getProfileId(), task.getTaskId(), schedule);
    }

    public void decorateTaskName(DTOTaskState taskState) {
        if (taskState == null) {
            return;
        }
        try {
            TpDailyTaskEnum taskEnum = TpDailyTaskEnum.fromId(taskState.getTaskId());
            taskState.setTaskName(taskEnum.getName());
        } catch (Exception e) {
            taskState.setTaskName("Unknown Task");
        }
    }
}
