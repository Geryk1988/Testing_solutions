package com.ubs.dqs.idq_gsnowintg.service;

import com.ubs.dqs.idq_gsnowintg.config.ConsumerProperties;
import com.ubs.dqs.idq_gsnowintg.model.IncidentRecord;
import com.ubs.dqs.idq_gsnowintg.util.DateTimeFormatterUtil;
import com.ubs.dqs.idq_gsnowintg.util.JsonParserUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Parses a raw Kafka message string into an IncidentRecord.
 * Pure business logic — no I/O here.
 * Java 1.8 compatible.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessorService {

    private final JsonParserUtil        jsonParser;
    private final DateTimeFormatterUtil dtFormatter;
    private final ConsumerProperties    props;

    /**
     * Attempts to parse rawMessage into an IncidentRecord.
     *
     * @return populated record if message passes filters, null otherwise
     */
    public IncidentRecord process(String rawMessage) {
        // Step 1: extract first JSON object
        String jsonString = jsonParser.extractFirstJsonObject(rawMessage);
        if (jsonString == null) {
            return null;
        }

        // Step 2: clean backslashes / BOM
        jsonString = jsonParser.removeBackslashesAndBOM(jsonString);

        // Step 3: apply detection filters
        if (!matchesFilters(jsonString)) {
            return null;
        }

        // Step 4: extract all 10 fields
        return extractRecord(jsonString);
    }

    // ── Detection ─────────────────────────────────────────────────────────────

    private boolean matchesFilters(String jsonString) {
        String detectStr = props.getDetectString();
        boolean containsDetectString = detectStr != null
            && !detectStr.isEmpty()
            && jsonString.contains(detectStr);

        boolean containsAnySWC = false;
        String[] swcs = props.getDetectSwcs();
        if (swcs != null && swcs.length > 0) {
            for (String swc : swcs) {
                if (swc != null && !swc.trim().isEmpty()
                        && jsonString.contains(swc.trim())) {
                    containsAnySWC = true;
                    break;
                }
            }
        }

        return containsDetectString && containsAnySWC;
    }

    // ── Field extraction (sections 1–10 from original code) ───────────────────

    private IncidentRecord extractRecord(String json) {

        // 1. Incident number
        String number = jsonParser.extractJsonValue(json, "\"number\":\"", "\"");

        // 2. Service offering
        String serviceOffering = jsonParser.extractJsonValue(json,
            "\"service_offering\":{\"display_value\":\"", "\"");

        // 3. Short description
        String shortDescription = jsonParser.extractJsonValue(json,
            "\"short_description\":\"", "\"");

        // 4. MSG_TIMESTAMP
        String rawTimestamp = jsonParser.extractJsonValue(json,
            "\"messageTimestampMs\":\"", "\"");
        String formattedTimestamp = dtFormatter.formatEpochMs(rawTimestamp);

        // 5. State
        String state = jsonParser.extractJsonValue(json,
            "\"state\":{\"display_value\":\"", "\"");

        // 6. CREATE_DATE (sys_created_on)
        String sysCreatedOnRaw = jsonParser.extractJsonValue(json,
            "\"sys_created_on\":{\"display_value\":\"", "\"");
        String formattedCreatedOn = dtFormatter.formatIsoDatetime(sysCreatedOnRaw, "CREATE_DATE");

        // 7. END_DATE (resolved_at)
        String resolvedAtRaw = jsonParser.extractJsonValue(json,
            "\"resolved_at\":{\"display_value\":\"", ",");
        String formattedClosedAt = dtFormatter.formatIsoDatetime(resolvedAtRaw, "END_DATE");

        // 8. ASSIGNED_TO display value
        String assignedToDisplay = jsonParser.extractJsonValue(json,
            "\"assigned_to\":{\"display_value\":\"", "\"");

        // 9. ASSIGNED_TO user_name — locate the assigned_to block first
        String assignedToBlock = jsonParser.extractJsonValue(json,
            "\"assigned_to\":{", "}");
        String assignedToUserName = jsonParser.extractJsonValue(
            "{" + assignedToBlock + "}", "\"user_name\":\"", "\"");

        // 10. Assignment group
        String assignmentGroup = jsonParser.extractJsonValue(json,
            "\"assignment_group\":{\"display_value\":\"", "\"");

        return IncidentRecord.builder()
            .number(number)
            .serviceOffering(serviceOffering)
            .shortDescription(shortDescription)
            .formattedTimestamp(formattedTimestamp)
            .state(state)
            .formattedCreatedOn(formattedCreatedOn)
            .formattedClosedAt(formattedClosedAt)
            .assignedTo(assignedToDisplay)
            .assignedToUserName(assignedToUserName)
            .assignmentGroup(assignmentGroup)
            .build();
    }
}
