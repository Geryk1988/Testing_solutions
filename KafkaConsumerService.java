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

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

    private final MessageProcessorService       messageProcessor;
    private final FileOutputService             fileOutputService;
    private final ConsumerStatsService          stats;
    private final ConsumerProperties            props;
    private final ConsumerShutdownScheduler     shutdownScheduler;

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

        // Dodatkowy guard — gdy wiadomości przychodzą po upływie czasu
        // (scheduler może miec max 10s opoznienia)
        if (System.currentTimeMillis() > endTimeMs) {
            ack.acknowledge();
            shutdownScheduler.initiateShutdown();
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
}
