package cl.camodev.wosbot.web.api;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.camodev.wosbot.ot.DTOTaskStatsAggregate;
import cl.camodev.wosbot.serv.impl.ServTaskStats;

@RestController
@RequestMapping("/api/stats")
public class StatsApi {

    private static final Logger logger = LoggerFactory.getLogger(StatsApi.class);

    @GetMapping("/tasks")
    public ResponseEntity<?> getTaskStats(
            @RequestParam(name = "profileId", required = false) Long profileId,
            @RequestParam(name = "taskId", required = false) Integer taskId,
            @RequestParam(name = "limit", required = false, defaultValue = "1000") Integer limit) {
        try {
            int effectiveLimit = (limit != null && limit > 0) ? limit : 1000;
            logger.info("Fetching task execution stats (profileId={}, taskId={}, limit={})",
                    profileId, taskId, effectiveLimit);

            List<DTOTaskStatsAggregate> data = ServTaskStats.getInstance()
                    .getTaskSummaries(profileId, taskId, effectiveLimit);

            Map<String, Object> meta = new HashMap<>();
            meta.put("profileId", profileId);
            meta.put("taskId", taskId);
            meta.put("limit", effectiveLimit);
            meta.put("groups", data.size());

            return ResponseEntity.ok(Map.of("meta", meta, "data", data));
        } catch (Exception e) {
            logger.error("Error fetching task execution stats: {}", e.getMessage(), e);
            return ResponseEntity.status(500)
                    .body(Map.of("error", "Failed to fetch task stats", "message", e.getMessage()));
        }
    }
}
