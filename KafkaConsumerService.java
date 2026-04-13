package com.ubs.dqs.idq_gsnowintg.service;

import com.ubs.dqs.idq_gsnowintg.config.ConsumerProperties;
import com.ubs.dqs.idq_gsnowintg.model.IncidentRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.MDC;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;

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

    private final MessageProcessorService messageProcessor;
    private final FileOutputService       fileOutputService;
    private final ConsumerStatsService    stats;
    private final ConsumerProperties      props;

    private long endTimeMs;

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
            log.info("[RunID:{}] ***Completed Duration — stopping consumer***", stats.getRunId());
            stats.markStopped();
            ack.acknowledge();
            flushAndSummarise();
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
