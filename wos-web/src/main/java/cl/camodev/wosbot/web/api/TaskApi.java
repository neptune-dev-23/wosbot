package cl.camodev.wosbot.web.api;

import cl.camodev.wosbot.ot.DTOTaskState;
import cl.camodev.wosbot.web.task.TaskDataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

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
}
