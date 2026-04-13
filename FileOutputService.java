package com.ubs.dqs.idq_gsnowintg.service;

import com.ubs.dqs.idq_gsnowintg.config.ConsumerProperties;
import com.ubs.dqs.idq_gsnowintg.model.IncidentRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Manages the two output files:
 *   1. Consumer output file  — hash-delimited rows
 *   2. JSON dump file        — raw JSON (timestamped filename)
 *
 * Writers opened once at construction, flushed/closed on Spring shutdown.
 * Java 1.8 compatible.
 */
@Slf4j
@Service
public class FileOutputService implements Closeable {

    private final String         outputFilePath;
    private final BufferedWriter hashWriter;
    private final BufferedWriter jsonWriter;

    public FileOutputService(ConsumerProperties props) throws IOException {
        this.outputFilePath = props.getOutputFile();

        // Build timestamped JSON dump path (matches original behaviour)
        String timestamp = ZonedDateTime.now(ZoneOffset.UTC)
            .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String jsonDumpPath = props.getJsonDumpBasePath() + timestamp + ".json";

        // Clean up any stale files from a previous run
        deleteIfExists(outputFilePath);
        deleteIfExists(jsonDumpPath);

        hashWriter = new BufferedWriter(new FileWriter(outputFilePath, true));
        jsonWriter = new BufferedWriter(new FileWriter(jsonDumpPath,   true));

        log.info("Output files initialised: hash={} json={}", outputFilePath, jsonDumpPath);
    }

    /** Writes one hash-delimited row and a newline. */
    public synchronized void writeHashRow(String runId, IncidentRecord record) throws IOException {
        String row = record.toHashRow();
        hashWriter.write(row);
        hashWriter.newLine();
        log.info("[RunID:{}] Written hash-separated row: {}", runId, row);
    }

    /** Dumps the raw JSON string to the GSNOW output file. */
    public synchronized void writeJsonDump(String jsonString) throws IOException {
        jsonWriter.write(jsonString);
        jsonWriter.newLine();
    }

    /** Flushes both writers — called after the poll loop exits. */
    public void flush() throws IOException {
        hashWriter.flush();
        jsonWriter.flush();
    }

    /** Returns the byte size of the consumer output file. */
    public long outputFileSizeBytes() {
        return new File(outputFilePath).length();
    }

    /** Counts lines in the consumer output file. */
    public long outputFileLineCount() {
        try (BufferedReader reader = new BufferedReader(new FileReader(outputFilePath))) {
            long count = 0;
            while (reader.readLine() != null) count++;
            return count;
        } catch (IOException e) {
            log.error("Error counting lines in output file: {}", e.getMessage());
            return -1;
        }
    }

    @Override
    @PreDestroy
    public void close() {
        try { hashWriter.close(); } catch (IOException e) { log.warn("Error closing hashWriter", e); }
        try { jsonWriter.close(); } catch (IOException e) { log.warn("Error closing jsonWriter", e); }
    }

    private void deleteIfExists(String path) {
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e) {
            log.warn("Could not delete existing file {}: {}", path, e.getMessage());
        }
    }
}
