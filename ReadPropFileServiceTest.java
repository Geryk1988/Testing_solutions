package com.ubs.dqs.idq_gsnowintg;

import com.ubs.dqs.idq_gsnowintg.service.ReadPropFileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReadPropFileServiceTest {

    @TempDir
    Path tempDir;

    private ReadPropFileService service;

    @BeforeEach
    void setUp() {
        service = new ReadPropFileService();
    }

    @Test
    void load_parsesKeyValuePairs() throws Exception {
        File propsFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter fw = new FileWriter(propsFile)) {
            fw.write("CONSUMER_BROKERS=localhost:9093\n");
            fw.write("CONSUMER_GROUPID=my-group\n");
            fw.write("# this is a comment\n");
            fw.write("\n");
            fw.write("DETECT_STRING=actimize\n");
        }

        service.setPath(propsFile.getAbsolutePath());
        service.load();

        assertThat(service.get("CONSUMER_BROKERS")).isEqualTo("localhost:9093");
        assertThat(service.get("CONSUMER_GROUPID")).isEqualTo("my-group");
        assertThat(service.get("DETECT_STRING")).isEqualTo("actimize");
    }

    @Test
    void load_ignoresCommentsAndBlankLines() throws Exception {
        File propsFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter fw = new FileWriter(propsFile)) {
            fw.write("# comment line\n");
            fw.write("\n");
            fw.write("KEY=value\n");
        }

        service.setPath(propsFile.getAbsolutePath());
        service.load();

        assertThat(service.get("KEY")).isEqualTo("value");
        assertThat(service.get("# comment line")).isNull();
    }

    @Test
    void getOrDefault_returnsDefaultWhenKeyAbsent() throws Exception {
        File propsFile = tempDir.resolve("test.properties").toFile();
        try (FileWriter fw = new FileWriter(propsFile)) {
            fw.write("EXISTING=yes\n");
        }

        service.setPath(propsFile.getAbsolutePath());
        service.load();

        assertThat(service.getOrDefault("MISSING_KEY", "fallback")).isEqualTo("fallback");
        assertThat(service.getOrDefault("EXISTING",    "fallback")).isEqualTo("yes");
    }

    @Test
    void load_throwsWhenPathIsNull() {
        service.setPath(null);
        assertThatThrownBy(() -> service.load())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("propFileName");
    }
}
