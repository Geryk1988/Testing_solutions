package com.actimize.kafka.service;

import com.actimize.kafka.config.ConsumerProperties;
import com.actimize.kafka.model.IncidentRecord;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;

/**
 * Core Kafka listener.
 *
 * Spring manages the poll loop, thread lifecycle, and offset commits.
 * This class receives pre-polled records and delegates to
 * {@link MessageProcessorService} (parsing/filtering) and
 * {@link FileOutputService} (writing).
 *
 * Duration enforcement is done via a simple wall-clock check on each record —
 * consistent with the original while(!closed && currentTimeMillis() > end) logic.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final MessageProcessorService messageProcessor;
    private final FileOutputService       fileOutputService;
    private final ConsumerStatsService    stats;
    private final ConsumerProperties      props;

    private long endTimeMs;

    @PostConstruct
    void init() {
        endTimeMs = System.currentTimeMillis() + props.getDurationMs();
        MDC.put("runId", stats.getRunId());
        log.info("[RunID:{}] KafkaConsumerService initialised. Duration={}ms, Topic={}",
            stats.getRunId(), props.getDurationMs(), props.getTopic());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Listener
    // ─────────────────────────────────────────────────────────────────────────

    @KafkaListener(
        topics          = "${consumer.topic}",
        groupId         = "${consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {

        // ── Duration check ────────────────────────────────────────────────────
        if (System.currentTimeMillis() > endTimeMs) {
            log.info("[RunID:{}] ***Completed Duration — stopping consumer***", stats.getRunId());
            stats.markStopped();
            ack.acknowledge();
            flushAndSummarise();
            return;
        }

        String rawMessage = record.value();

        // ── Process ───────────────────────────────────────────────────────────
        try {
            IncidentRecord incident = messageProcessor.process(rawMessage);

            if (incident == null) {
                // Either JSON extraction failed OR filters did not match.
                // MessageProcessorService logged the reason; we just count.
                handleSkipped(rawMessage);
            } else {
                stats.incrementTotalChecked();
                writeOutput(incident, rawMessage);
                stats.incrementFilteredCount();
            }

        } catch (Exception e) {
            log.error("[RunID:{}] Unexpected error processing record offset={}: {}",
                stats.getRunId(), record.offset(), e.getMessage(), e);
            stats.incrementErrorCount();
        }

        ack.acknowledge();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private void handleSkipped(String rawMessage) {
        // If extractFirstJsonObject returned null it's a hard error;
        // if filters didn't match it's a silent skip. We count both as
        // "not written" — errorCount vs filteredOut is separated in stats.
        stats.incrementErrorCount();
    }

    private void writeOutput(IncidentRecord incident, String rawJson) throws IOException {
        fileOutputService.writeHashRow(stats.getRunId(), incident);
        fileOutputService.writeJsonDump(rawJson);
    }

    private void flushAndSummarise() {
        try {
            fileOutputService.flush();
        } catch (IOException e) {
            log.error("[RunID:{}] Error flushing output files: {}", stats.getRunId(), e.getMessage());
        }

        long sizeBytes = fileOutputService.outputFileSizeBytes();
        long lineCount = fileOutputService.outputFileLineCount();

        log.info("[RunID:{}] Consumer Output File size (bytes): {}", stats.getRunId(), sizeBytes);
        log.info("[RunID:{}] Consumer Output File line count  : {}", stats.getRunId(), lineCount);

        stats.markStopped();
        stats.logSummary();
    }
}
