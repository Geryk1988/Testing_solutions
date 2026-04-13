package com.ubs.dqs.idq_gsnowintg.config;

import com.ubs.dqs.idq_gsnowintg.service.ReadPropFileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * Reads all Kafka consumer run-time parameters from the external
 * GSNOW_RunProperties_CFLNT.properties file via ReadPropFileService.
 *
 * Property keys match those used in the original EventsConsumerwithSASLSSL.
 */
@Slf4j
@Getter
@Component
public class ConsumerProperties {

    private final ReadPropFileService props;

    // ── Kafka connection ──────────────────────────────────────────────────────
    private String brokers;
    private String groupId;
    private String topic;
    private String apiKey;
    private String apiSecret;
    private String schemaRegistryUrl;
    private String schemaRegistryApiKey;
    private String schemaRegistryApiSecret;
    private String trustStorePath;

    // ── Poll tuning ───────────────────────────────────────────────────────────
    private int    pollDurationSeconds;
    private int    maxPollRecords;
    private int    sessionTimeoutMs;
    private int    heartbeatIntervalMs;
    private int    maxPollIntervalMs;

    // ── Detection filters ─────────────────────────────────────────────────────
    private String   detectString;
    private String[] detectSwcs;

    // ── Output files ──────────────────────────────────────────────────────────
    private String outputFile;
    private String jsonDumpBasePath;

    // ── Run duration ──────────────────────────────────────────────────────────
    private long   duration;
    private String durationUnit;

    @Autowired
    public ConsumerProperties(ReadPropFileService props) {
        this.props = props;
    }

    /**
     * Runs after ReadPropFileService.load() has populated the map.
     * Spring guarantees @PostConstruct ordering within the same context:
     * ReadPropFileService is initialised first (it is a dependency),
     * then this method fires.
     */
    @PostConstruct
    public void init() {
        brokers               = require("CONSUMER_BROKERS");
        groupId               = require("CONSUMER_GROUPID");
        topic                 = require("CONSUMER_TOPIC");
        apiKey                = require("CONSUMER_API_KEY");
        apiSecret             = require("CONSUMER_API_SECRET");
        schemaRegistryUrl     = props.get("CONSUMER_SR_URL");
        schemaRegistryApiKey  = props.get("CONSUMER_SR_API_KEY");
        schemaRegistryApiSecret = props.get("CONSUMER_SR_API_SECRET");
        trustStorePath        = props.get("TRUST_STORE");

        pollDurationSeconds   = intOrDefault("POLL_DURATION_SECONDS", 1);
        maxPollRecords        = intOrDefault("MAX_POLL_RECORDS", 100);
        sessionTimeoutMs      = intOrDefault("SESSION_TIMEOUT_MS", 30_000);
        heartbeatIntervalMs   = intOrDefault("HEARTBEAT_INTERVAL_MS", 10_000);
        maxPollIntervalMs     = intOrDefault("MAX_POLL_INTERVAL_MS", 600_000);

        detectString          = props.getOrDefault("DETECT_STRING", "");
        String swcRaw         = props.getOrDefault("DETECT_SWC", "");
        detectSwcs            = swcRaw.isEmpty() ? new String[0] : swcRaw.split(",");

        outputFile            = require("CONSUMER_OUTPUT_FILE");
        jsonDumpBasePath      = require("GSNOW_OUTPUT_FILE");

        duration              = longOrDefault("DURATION", 60L);
        durationUnit          = props.getOrDefault("DURATION_UNIT", "m");

        log.info("ConsumerProperties loaded: brokers={} group={} topic={}", brokers, groupId, topic);
    }

    /** Converts duration + unit to milliseconds. */
    public long getDurationMs() {
        switch (durationUnit.toLowerCase().charAt(0)) {
            case 's': return duration * 1_000L;
            case 'h': return duration * 60L * 60L * 1_000L;
            default:  return duration * 60L * 1_000L;   // 'm' is the default
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String require(String key) {
        String v = props.get(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("Required property '" + key + "' is missing from the run-properties file");
        }
        return v;
    }

    private int intOrDefault(String key, int def) {
        try { return Integer.parseInt(props.getOrDefault(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }

    private long longOrDefault(String key, long def) {
        try { return Long.parseLong(props.getOrDefault(key, String.valueOf(def))); }
        catch (NumberFormatException e) { return def; }
    }
}
