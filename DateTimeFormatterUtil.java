package com.ubs.dqs.idq_gsnowintg.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Centralised date/time formatting helpers.
 * Java 1.8 compatible — uses java.time (available since Java 8).
 */
@Slf4j
@Component
public class DateTimeFormatterUtil {

    private static final DateTimeFormatter ISO_OFFSET_IN =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    private static final DateTimeFormatter DATETIME_OUT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final DateTimeFormatter TIMESTAMP_OUT =
        DateTimeFormatter.ofPattern("dd-MM-yyyy'T'HH:mm:ss.SSSZ");

    /**
     * Formats an epoch-millisecond string (MSG_TIMESTAMP) into
     * dd-MM-yyyy'T'HH:mm:ss.SSSZ (UTC).
     */
    public String formatEpochMs(String epochMs) {
        if (epochMs == null || epochMs.trim().isEmpty()) return "";
        try {
            long ts = Long.parseLong(epochMs);
            Instant inst = Instant.ofEpochMilli(ts);
            ZonedDateTime zdt = inst.atZone(ZoneOffset.UTC);
            return zdt.format(TIMESTAMP_OUT);
        } catch (NumberFormatException e) {
            log.warn("Could not parse epoch ms '{}', returning original", epochMs);
            return epochMs;
        }
    }

    /**
     * Parses an ISO-offset datetime string (e.g. 2026-01-12T10:44:44.001+0000)
     * and reformats it as yyyy-MM-dd HH:mm:ss.
     */
    public String formatIsoDatetime(String isoDatetime, String fieldName) {
        if (isoDatetime == null || isoDatetime.trim().isEmpty()) {
            log.info("{} is blank / empty — writing empty string", fieldName);
            return "";
        }

        // Strip leading/trailing quotes that may survive JSON extraction
        String value = isoDatetime.trim();
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() > 1) {
            value = value.substring(1, value.length() - 1);
        }

        try {
            ZonedDateTime zdt = ZonedDateTime.parse(value, ISO_OFFSET_IN);
            return zdt.format(DATETIME_OUT);
        } catch (Exception e) {
            log.warn("Could not parse {} datetime '{}': {}", fieldName, value, e.getMessage());
            return "";
        }
    }
}
