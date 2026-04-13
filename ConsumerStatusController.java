package com.ubs.dqs.idq_gsnowintg.controller;

import com.ubs.dqs.idq_gsnowintg.model.ConsumerRunStats;
import com.ubs.dqs.idq_gsnowintg.service.ConsumerStatsService;
import com.ubs.dqs.idq_gsnowintg.service.FileOutputService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * REST endpoints exposing consumer run status.
 * Runs on port 8079 (set in application.properties).
 *
 *   GET /api/consumer/status  — full run statistics snapshot
 *   GET /api/consumer/health  — simple up/down liveness check
 *
 * Java 1.8 compatible.
 */
@RestController
@RequestMapping("/api/consumer")
@RequiredArgsConstructor
public class ConsumerStatusController {

    private final ConsumerStatsService stats;
    private final FileOutputService    fileOutputService;

    @GetMapping("/status")
    public ResponseEntity<ConsumerRunStats> status() {
        ConsumerRunStats snapshot = stats.snapshot(
            fileOutputService.outputFileSizeBytes(),
            fileOutputService.outputFileLineCount()
        );
        return ResponseEntity.ok(snapshot);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        boolean running = stats.isRunning();

        Map<String, Object> body = new HashMap<>();
        body.put("status",  running ? "RUNNING" : "STOPPED");
        body.put("runId",   stats.getRunId());
        body.put("started", stats.getStartTime().toString());

        return running
            ? ResponseEntity.ok(body)
            : ResponseEntity.status(503).body(body);
    }
}
