package com.ubs.dqs.idq_gsnowintg.service;

import com.ubs.dqs.idq_gsnowintg.model.ConsumerRunStats;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe counters for the current consumer run.
 * Exposed via the REST status endpoint and logged at run completion.
 * Java 1.8 compatible.
 */
@Slf4j
@Service
public class ConsumerStatsService {

    @Getter
    private final String      runId;
    @Getter
    private final Instant     startTime;

    private final AtomicInteger totalChecked  = new AtomicInteger();
    private final AtomicInteger filteredCount = new AtomicInteger();
    private final AtomicInteger errorCount    = new AtomicInteger();
    private final AtomicBoolean running       = new AtomicBoolean(true);

    @Getter
    private Instant endTime;

    public ConsumerStatsService() {
        this.startTime = Instant.now();
        this.runId     = System.currentTimeMillis() + "-" + (int)(Math.random() * 10_000);
        log.info("Consumer run started. RunID={}", runId);
    }

    public boolean isRunning() { return running.get(); }

    public void incrementTotalChecked()  { totalChecked.incrementAndGet(); }
    public void incrementFilteredCount() { filteredCount.incrementAndGet(); }
    public void incrementErrorCount()    { errorCount.incrementAndGet(); }

    public void markStopped() {
        running.set(false);
        endTime = Instant.now();
    }

    /** Builds a read-only snapshot for the REST endpoint or final log. */
    public ConsumerRunStats snapshot(long fileSizeBytes, long fileLineCount) {
        Instant end  = endTime != null ? endTime : Instant.now();
        long    ms   = end.toEpochMilli() - startTime.toEpochMilli();
        int     tc   = totalChecked.get();
        int     fc   = filteredCount.get();
        int     ec   = errorCount.get();

        return ConsumerRunStats.builder()
            .runId(runId)
            .startTime(startTime)
            .endTime(end)
            .totalChecked(tc)
            .filteredCount(fc)
            .errorCount(ec)
            .filteredOutCount(tc - fc - ec)
            .durationMs(ms)
            .durationSeconds(ms / 1_000.0)
            .durationMinutes(ms / 1_000.0 / 60.0)
            .durationHours(ms / 1_000.0 / 60.0 / 60.0)
            .outputFileSizeBytes(fileSizeBytes)
            .outputFileLineCount(fileLineCount)
            .running(running.get())
            .build();
    }

    /** Logs the final summary — mirrors the original LOGGER.info block. */
    public void logSummary() {
        Instant end = endTime != null ? endTime : Instant.now();
        long    ms  = end.toEpochMilli() - startTime.toEpochMilli();
        int     tc  = totalChecked.get();
        int     fc  = filteredCount.get();
        int     ec  = errorCount.get();

        String sep = new String(new char[100]).replace("\0", "-");
        log.info(sep);
        log.info("[RunID:{}] Summary for this run:", runId);
        log.info(sep);
        log.info("[RunID:{}] Total valid JSON rows checked        : {}", runId, tc);
        log.info("[RunID:{}] Total filtered (written) rows        : {}", runId, fc);
        log.info("[RunID:{}] Total errors (could not extract JSON): {}", runId, ec);
        log.info("[RunID:{}] Total filtered out (not written) rows: {}", runId, tc - fc - ec);
        log.info(sep);
        log.info("[RunID:{}] Consumer started at : {}", runId, startTime);
        log.info("[RunID:{}] Consumer ended at   : {}", runId, end);
        log.info(sep);
        log.info("[RunID:{}] Total run duration (ms)     : {}", runId, ms);
        log.info("[RunID:{}] Total run duration (seconds): {}", runId, String.format("%.2f", ms / 1_000.0));
        log.info("[RunID:{}] Total run duration (minutes): {}", runId, String.format("%.2f", ms / 60_000.0));
        log.info("[RunID:{}] Total run duration (hours)  : {}", runId, String.format("%.2f", ms / 3_600_000.0));
        log.info(sep);
    }
}
