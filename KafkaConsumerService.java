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

    private long endTimeMs;
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

    private void initiateShutdown() {
        if (!shutdownInitiated.compareAndSet(false, true)) {
            return;
        }

        log.info("[RunID:{}] ***Completed Duration — stopping consumer***", stats.getRunId());
        stats.markStopped();
        flushAndSummarise();

        // Shutdown musi byc w osobnym watku — wywolanie System.exit()
        // z watku Kafka listener powoduje deadlock podczas zamykania kontekstu
        Thread shutdownThread = new Thread(() -> {
            try {
                log.info("[RunID:{}] Stopping Kafka listener containers...", stats.getRunId());
                listenerRegistry.stop();

                log.info("[RunID:{}] Shutting down Spring application...", stats.getRunId());
                int exitCode = SpringApplication.exit(applicationContext, () -> 0);
                System.exit(exitCode);
            } catch (Exception e) {
                log.error("[RunID:{}] Error during shutdown: {}", stats.getRunId(), e.getMessage(), e);
                System.exit(1);
            }
        }, "consumer-shutdown-thread");

        shutdownThread.setDaemon(false); // nie-daemon: JVM poczeka az watek skonczy
        shutdownThread.start();
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
