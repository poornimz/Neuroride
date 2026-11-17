package com.neuroride.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AiInsightController {

    private final SimpMessagingTemplate messagingTemplate;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/internal/rides/{rideId}/insights")
    public ResponseEntity<Void> broadcastInsight(
            @PathVariable UUID rideId,
            @RequestBody Map<String, Object> insightPayload) {
        messagingTemplate.convertAndSend("/topic/ride." + rideId + ".insights", insightPayload);
        return ResponseEntity.accepted().build();
    }

    @GetMapping("/rides/{rideId}/insights")
    public ResponseEntity<List<Map<String, Object>>> getInsights(@PathVariable UUID rideId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT id, insight_type, summary_text, raw_metadata, model_version, created_at " +
                "FROM ride_ai_insights WHERE ride_id = ? ORDER BY created_at DESC",
                rideId
        );
        return ResponseEntity.ok(rows);
    }
}