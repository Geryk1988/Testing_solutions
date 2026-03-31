package com.actimize.kafka;

import com.actimize.kafka.config.ConsumerProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the Actimize Kafka Consumer microservice.
 *
 * Run with:
 *   java -jar kafka-consumer-service.jar
 *
 * Or pass an external properties file:
 *   java -jar kafka-consumer-service.jar \
 *        --spring.config.additional-location=file:/path/to/consumer.properties
 */
@Slf4j
@SpringBootApplication
@EnableConfigurationProperties(ConsumerProperties.class)
public class KafkaConsumerApplication {

    public static void main(String[] args) {
        log.info("Starting Actimize Kafka Consumer Service...");
        SpringApplication.run(KafkaConsumerApplication.class, args);
    }
}
