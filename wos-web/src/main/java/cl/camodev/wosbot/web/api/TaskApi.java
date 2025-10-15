package cl.camodev.wosbot.web.api;

import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.web.task.TaskDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST API controller for task management operations.
 */
@RestController
@RequestMapping("/api")
public class TaskApi {

    private static final Logger logger = LoggerFactory.getLogger(TaskApi.class);
    private final TaskDataService taskDataService;

    public TaskApi(TaskDataService taskDataService) {
        this.taskDataService = taskDataService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<?> getTasks() {
        try {
            logger.info("Fetching tasks for all profiles");
            Map<Long, List<DTOTaskState>> tasksMap = taskDataService.getTasksByProfile();
            return ResponseEntity.ok(tasksMap);
        } catch (Exception e) {
            logger.error("Error fetching tasks: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to fetch tasks", "message", e.getMessage()));
        }
    }

    @PostMapping("/tasks/reschedule")
    public ResponseEntity<?> rescheduleTask(@RequestBody DTOTaskState task) {
        try {
            logger.info("Rescheduling task {} for profile {}", task.getTaskId(), task.getProfileId());
            taskDataService.rescheduleTask(task);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid reschedule request: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid task data", "message", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rescheduling task: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                .body(Map.of("error", "Failed to reschedule task", "message", e.getMessage()));
        }
    }
}
