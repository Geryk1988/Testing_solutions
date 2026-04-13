package com.ubs.dqs.idq_gsnowintg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the idq-gsnowintg Kafka Consumer microservice.
 *
 * The external run-properties file path is configured in application.properties
 * via runprops.path and loaded by ReadPropFileService on startup.
 *
 * Run with:
 *   java -jar idq-gsnowintg-0.0.1-SNAPSHOT.jar
 *
 * Override the properties file path at runtime:
 *   java -jar idq-gsnowintg-0.0.1-SNAPSHOT.jar \
 *        --runprops.path=/path/to/GSNOW_RunProperties_CFLNT.properties
 */
@Slf4j
@SpringBootApplication
public class IdqGsnowintgApplication {

    public static void main(String[] args) {
        log.info("Starting idq-gsnowintg Kafka Consumer Service...");
        SpringApplication.run(IdqGsnowintgApplication.class, args);
    }
}
