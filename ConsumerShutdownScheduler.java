package com.ubs.dqs.idq_gsnowintg.service;

import com.ubs.dqs.idq_gsnowintg.config.ConsumerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sprawdza co 10 sekund czy upłynął skonfigurowany czas działania.
 * Działa niezależnie od ruchu na topicu — rozwiązuje problem gdy
 * brak wiadomości na Kafce powoduje że onMessage() nigdy nie jest wywoływany.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsumerShutdownScheduler {

    private final ConsumerProperties            props;
    private final ConsumerStatsService          stats;
    private final FileOutputService             fileOutputService;
    private final KafkaListenerEndpointRegistry listenerRegistry;
    private final ApplicationContext            applicationContext;

    private long endTimeMs;
    private final AtomicBoolean shutdownInitiated = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        endTimeMs = System.currentTimeMillis() + props.getDurationMs();
        log.info("[RunID:{}] Shutdown scheduler initialised. Will stop at: {} ms from epoch",
            stats.getRunId(), endTimeMs);
    }

    /**
     * Odpala co 10 sekund (10000 ms).
     * Możesz zmienić fixedDelay na mniejszą wartość jeśli chcesz
     * bardziej precyzyjne zatrzymanie.
     */
    @Scheduled(fixedDelay = 10_000)
    public void checkDuration() {
        if (shutdownInitiated.get()) {
            return;
        }

        if (System.currentTimeMillis() > endTimeMs) {
            initiateShutdown();
        }
    }

    public void initiateShutdown() {
        if (!shutdownInitiated.compareAndSet(false, true)) {
            return;
        }

        log.info("[RunID:{}] ***Completed Duration — stopping consumer***", stats.getRunId());
        stats.markStopped();
        flushAndSummarise();

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

        shutdownThread.setDaemon(false);
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
