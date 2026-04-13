package com.ubs.dqs.idq_gsnowintg;

import com.ubs.dqs.idq_gsnowintg.util.JsonParserUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JsonParserUtilTest {

    private JsonParserUtil util;

    @BeforeEach
    void setUp() {
        util = new JsonParserUtil();
    }

    // ── extractFirstJsonObject ────────────────────────────────────────────────

    @Test
    void extractFirstJsonObject_returnsNullForNullInput() {
        assertThat(util.extractFirstJsonObject(null)).isNull();
    }

    @Test
    void extractFirstJsonObject_returnsNullWhenNoBrace() {
        assertThat(util.extractFirstJsonObject("no braces here")).isNull();
    }

    @Test
    void extractFirstJsonObject_extractsSimpleObject() {
        String input = "some prefix {\"key\":\"value\"} trailing";
        assertThat(util.extractFirstJsonObject(input)).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void extractFirstJsonObject_handlesNestedObjects() {
        String input = "{\"outer\":{\"inner\":\"val\"}}";
        assertThat(util.extractFirstJsonObject(input)).isEqualTo(input);
    }

    @Test
    void extractFirstJsonObject_stripsBOM() {
        String input = "\uFEFF{\"key\":\"value\"}";
        assertThat(util.extractFirstJsonObject(input)).isEqualTo("{\"key\":\"value\"}");
    }

    @Test
    void extractFirstJsonObject_returnsNullForUnclosedBrace() {
        assertThat(util.extractFirstJsonObject("{\"unclosed\":\"brace\"")).isNull();
    }

    // ── removeBackslashesAndBOM ───────────────────────────────────────────────

    @Test
    void removeBackslashesAndBOM_returnsNullForNull() {
        assertThat(util.removeBackslashesAndBOM(null)).isNull();
    }

    @Test
    void removeBackslashesAndBOM_removesBackslashes() {
        assertThat(util.removeBackslashesAndBOM("a\\b\\c")).isEqualTo("abc");
    }

    @Test
    void removeBackslashesAndBOM_removesBOM() {
        assertThat(util.removeBackslashesAndBOM("\uFEFFhello")).isEqualTo("hello");
    }

    // ── extractJsonValue ──────────────────────────────────────────────────────

    @Test
    void extractJsonValue_returnsEmptyWhenMarkerAbsent() {
        assertThat(util.extractJsonValue("{\"a\":\"b\"}", "\"missing\":", "\"")).isEqualTo("");
    }

    @Test
    void extractJsonValue_extractsCorrectly() {
        String json = "{\"number\":\"INC0001234\",\"other\":\"x\"}";
        assertThat(util.extractJsonValue(json, "\"number\":\"", "\""))
            .isEqualTo("INC0001234");
    }

    @Test
    void extractJsonValue_handlesNullInputs() {
        assertThat(util.extractJsonValue(null,  "start", "end")).isEqualTo("");
        assertThat(util.extractJsonValue("{}",  null,    "end")).isEqualTo("");
        assertThat(util.extractJsonValue("{}",  "start", null )).isEqualTo("");
    }
}
