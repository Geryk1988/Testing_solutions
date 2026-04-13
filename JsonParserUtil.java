package com.ubs.dqs.idq_gsnowintg.util;

import org.springframework.stereotype.Component;

/**
 * Stateless JSON utility methods extracted from the original static helpers.
 * Java 1.8 compatible.
 */
@Component
public class JsonParserUtil {

    /**
     * Extracts the first complete JSON object from a raw Kafka message string.
     * Handles BOM characters and strips control characters before scanning.
     */
    public String extractFirstJsonObject(String input) {
        if (input == null) return null;

        // Strip UTF-8 BOM (String form \uFEFF)
        if (input.startsWith("\uFEFF")) {
            input = input.substring(1);
        }

        // Strip BOM bytes that survived as raw chars (0xEF 0xBB 0xBF)
        if (input.length() >= 3
                && input.charAt(0) == 0xEF
                && input.charAt(1) == 0xBB
                && input.charAt(2) == 0xBF) {
            input = input.substring(3);
        }

        // Remove C0 control characters (except tab, CR, LF)
        input = input.replaceAll("[\\x00-\\x1F&&[^\\r\\n\\t]]", "");

        int firstBrace = input.indexOf('{');
        if (firstBrace == -1) return null;

        int openBraces = 0;
        int end = -1;
        for (int i = firstBrace; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '{') openBraces++;
            if (c == '}') openBraces--;
            if (openBraces == 0) {
                end = i;
                break;
            }
        }

        return (end != -1) ? input.substring(firstBrace, end + 1) : null;
    }

    /**
     * Strips BOM character and all backslashes from a JSON string.
     */
    public String removeBackslashesAndBOM(String input) {
        if (input == null) return null;
        return input.replace("\uFEFF", "").replace("\\", "");
    }

    /**
     * Extracts the substring between startMarker and endMarker within json.
     * Returns an empty string if either marker is absent.
     */
    public String extractJsonValue(String json, String startMarker, String endMarker) {
        if (json == null || startMarker == null || endMarker == null) return "";
        int start = json.indexOf(startMarker);
        if (start == -1) return "";
        start += startMarker.length();
        int end = json.indexOf(endMarker, start);
        if (end == -1) return "";
        return json.substring(start, end);
    }
}
