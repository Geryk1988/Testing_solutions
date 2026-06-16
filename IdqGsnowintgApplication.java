package com.ubs.dqs.idq_gsnowintg;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class IdqGsnowintgApplication {

    public static void main(String[] args) {
        log.info("Starting idq-gsnowintg Kafka Consumer Service...");
        SpringApplication.run(IdqGsnowintgApplication.class, args);
    }
}
