package cl.camodev.wosbot.web.api;

import cl.camodev.wosbot.almac.entity.SubTaskExecutionStat;
import cl.camodev.wosbot.serv.SubTaskExecutionStatService;
import cl.camodev.wosbot.serv.impl.SubTaskExecutionStatServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sub-task-stats")
public class SubTaskStatsApi {

    private static final Logger logger = LoggerFactory.getLogger(SubTaskStatsApi.class);
    private final SubTaskExecutionStatService subTaskExecutionStatService = new SubTaskExecutionStatServiceImpl();

    @GetMapping
    public ResponseEntity<?> getSubTaskStats(
            @RequestParam(name = "profileId", required = false) Long profileId,
            @RequestParam(name = "taskId", required = false) String taskId,
            @RequestParam(name = "subTaskType", required = false) String subTaskType,
            @RequestParam(name = "limit", required = false, defaultValue = "1000") Integer limit) {
        try {
            int effectiveLimit = (limit != null && limit > 0) ? limit : 1000;
            logger.info("Fetching sub-task execution stats (profileId={}, taskId={}, subTaskType={}, limit={})",
                    profileId, taskId, subTaskType, effectiveLimit);

            List<SubTaskExecutionStat> data = subTaskExecutionStatService.getStats(profileId, taskId, subTaskType, effectiveLimit);

            Map<String, Object> meta = new HashMap<>();
            meta.put("profileId", profileId);
            meta.put("taskId", taskId);
            meta.put("subTaskType", subTaskType);
            meta.put("limit", effectiveLimit);
            meta.put("count", data.size());

            return ResponseEntity.ok(Map.of("meta", meta, "data", data));
        } catch (Exception e) {
            logger.error("Error fetching sub-task execution stats: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch sub-task stats", "message", e.getMessage()));
        }
    }
}
