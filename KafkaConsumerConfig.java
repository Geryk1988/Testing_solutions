package com.actimize.kafka.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Kafka configuration — replaces the manual Properties/KafkaConsumer
 * setup from the original code. Spring manages the consumer lifecycle,
 * poll loop, and commit strategy.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

    private final ConsumerProperties props;

    @Bean
    public Map<String, Object> consumerConfigs() {
        Map<String, Object> config = new HashMap<>();

        // ── Core connection ───────────────────────────────────────────────────
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,          props.getBrokers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG,                   props.getGroupId());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                   "org.apache.kafka.common.serialization.StringDeserializer");
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                   "org.apache.kafka.common.serialization.StringDeserializer");

        // ── SASL / SSL ────────────────────────────────────────────────────────
        config.put("security.protocol",    "SASL_SSL");
        config.put("sasl.mechanism",       "PLAIN");
        config.put("ssl.truststore.location", props.getTrustStorePath());

        String jaas = String.format(
            "org.apache.kafka.common.security.plain.PlainLoginModule required " +
            "username=\"%s\" password=\"%s\";",
            props.getApiKey(), props.getApiSecret()
        );
        config.put("sasl.jaas.config", jaas);

        // ── Schema registry ───────────────────────────────────────────────────
        if (props.getSchemaRegistryUrl() != null) {
            config.put("schema.registry.url", props.getSchemaRegistryUrl());
            config.put("basic.auth.credentials.source", "USER_INFO");
            config.put("basic.auth.user.info",
                props.getSchemaRegistryApiKey() + ":" + props.getSchemaRegistryApiSecret());
        }

        // ── Poll tuning ───────────────────────────────────────────────────────
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG,          props.getMaxPollRecords());
        config.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG,        props.getSessionTimeoutMs());
        config.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG,     props.getHeartbeatIntervalMs());
        config.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG,      props.getMaxPollIntervalMs());
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,        false);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,         "earliest");

        log.info("Kafka consumer config built for brokers={} group={}", props.getBrokers(), props.getGroupId());
        return config;
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs());
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        // Manual ack — mirrors the original consumer.commitSync()
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        return factory;
    }
}
