package com.ubs.dqs.idq_gsnowintg;

import com.ubs.dqs.idq_gsnowintg.util.DateTimeFormatterUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DateTimeFormatterUtilTest {

    private DateTimeFormatterUtil util;

    @BeforeEach
    void setUp() {
        util = new DateTimeFormatterUtil();
    }

    // ── formatEpochMs ─────────────────────────────────────────────────────────

    @Test
    void formatEpochMs_returnsEmptyForNull() {
        assertThat(util.formatEpochMs(null)).isEmpty();
    }

    @Test
    void formatEpochMs_returnsEmptyForBlank() {
        assertThat(util.formatEpochMs("   ")).isEmpty();
    }

    @Test
    void formatEpochMs_returnsOriginalForNonNumeric() {
        assertThat(util.formatEpochMs("not-a-number")).isEqualTo("not-a-number");
    }

    @Test
    void formatEpochMs_formatsKnownEpoch() {
        // 2026-01-12T00:00:00Z = 1736640000000 ms
        String result = util.formatEpochMs("1736640000000");
        assertThat(result).startsWith("12-01-2026T");
    }

    // ── formatIsoDatetime ─────────────────────────────────────────────────────

    @Test
    void formatIsoDatetime_returnsEmptyForNull() {
        assertThat(util.formatIsoDatetime(null, "TEST")).isEmpty();
    }

    @Test
    void formatIsoDatetime_returnsEmptyForBlank() {
        assertThat(util.formatIsoDatetime("", "TEST")).isEmpty();
    }

    @Test
    void formatIsoDatetime_parsesKnownDatetime() {
        String result = util.formatIsoDatetime("2026-01-12T10:44:44.001+0000", "CREATE_DATE");
        assertThat(result).isEqualTo("2026-01-12 10:44:44");
    }

    @Test
    void formatIsoDatetime_stripsQuotesBeforeParsing() {
        String result = util.formatIsoDatetime("\"2026-01-12T10:44:44.001+0000\"", "CREATE_DATE");
        assertThat(result).isEqualTo("2026-01-12 10:44:44");
    }

    @Test
    void formatIsoDatetime_returnsEmptyForUnparseable() {
        assertThat(util.formatIsoDatetime("not-a-date", "TEST")).isEmpty();
    }
}
