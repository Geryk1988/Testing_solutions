package com.ubs.dqs.idq_gsnowintg.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Snapshot of a single consumer run's statistics.
 * Returned by the REST status endpoint and logged at the end of a run.
 */
@Data
@Builder
public class ConsumerRunStats {

    private String  runId;
    private Instant startTime;
    private Instant endTime;

    private int  totalChecked;
    private int  filteredCount;
    private int  errorCount;
    private int  filteredOutCount;

    private long   durationMs;
    private double durationSeconds;
    private double durationMinutes;
    private double durationHours;

    private String outputFile;
    private long   outputFileSizeBytes;
    private long   outputFileLineCount;

    private boolean running;
}
