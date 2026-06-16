package com.ubs.dqs.idq_gsnowintg.service;

import com.ubs.dqs.idq_gsnowintg.config.ConsumerProperties;
import com.ubs.dqs.idq_gsnowintg.model.IncidentRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;
import org.springframework.boot.SpringApplication;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Core Kafka listener.
 *
 * Spring manages the poll loop, thread lifecycle, and offset commits.
 * Delegates to MessageProcessorService (parsing/filtering) and
 * FileOutputService (writing).
 *
 * Java 1.8 compatible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final MessageProcessorService        messageProcessor;
    private final FileOutputService              fileOutputService;
    private final ConsumerStatsService           stats;
    private final ConsumerProperties             props;
    private final KafkaListenerEndpointRegistry  listenerRegistry;
    private final ApplicationContext             applicationContext;

    private long          endTimeMs;
    // Guard so shutdown logic runs only once even if multiple messages
    // arrive in the same poll batch after the deadline.
    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        endTimeMs = System.currentTimeMillis() + props.getDurationMs();
        MDC.put("runId", stats.getRunId());
        log.info("[RunID:{}] KafkaConsumerService initialised. Duration={}ms Topic={}",
            stats.getRunId(), props.getDurationMs(), props.getTopic());
    }

    @KafkaListener(
        topics           = "#{consumerProperties.topic}",
        groupId          = "#{consumerProperties.groupId}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment ack) {

        // Duration guard
        if (System.currentTimeMillis() > endTimeMs) {
            ack.acknowledge();
            initiateShutdown();
            return;
        }

        String rawMessage = record.value();

        try {
            IncidentRecord incident = messageProcessor.process(rawMessage);

            if (incident == null) {
                stats.incrementErrorCount();
            } else {
                stats.incrementTotalChecked();
                fileOutputService.writeHashRow(stats.getRunId(), incident);
                fileOutputService.writeJsonDump(rawMessage);
                stats.incrementFilteredCount();
            }

        } catch (Exception e) {
            log.error("[RunID:{}] Unexpected error processing record offset={}: {}",
                stats.getRunId(), record.offset(), e.getMessage(), e);
            stats.incrementErrorCount();
        }

        ack.acknowledge();
    }

    /**
     * Called once when the configured duration has elapsed.
     * 1. Flushes and logs summary.
     * 2. Stops the Kafka listener container (no more polls).
     * 3. Exits the Spring application (JVM terminates cleanly).
     */
    private void initiateShutdown() {
        if (!shutdownInitiated.compareAndSet(false, true)) {
            return; // another thread already started shutdown
        }

        log.info("[RunID:{}] ***Completed Duration — stopping consumer***", stats.getRunId());
        stats.markStopped();

        flushAndSummarise();

        // Stop all Kafka listener containers so the poll loop ends immediately.
        log.info("[RunID:{}] Stopping Kafka listener containers...", stats.getRunId());
        listenerRegistry.stop();

        // Shut down the Spring context and exit the JVM with code 0.
        log.info("[RunID:{}] Shutting down Spring application...", stats.getRunId());
        int exitCode = SpringApplication.exit(applicationContext, () -> 0);
        System.exit(exitCode);
    }

    private void flushAndSummarise() {
        try {
            fileOutputService.flush();
        } catch (IOException e) {
            log.error("[RunID:{}] Error flushing output files: {}", stats.getRunId(), e.getMessage());
        }

        log.info("[RunID:{}] Consumer Output File size (bytes): {}",
            stats.getRunId(), fileOutputService.outputFileSizeBytes());
        log.info("[RunID:{}] Consumer Output File line count  : {}",
            stats.getRunId(), fileOutputService.outputFileLineCount());

        stats.logSummary();
    }
}
