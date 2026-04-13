package com.ubs.dqs.idq_gsnowintg.service;

import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@Data
@ToString
public class ReadPropFileService {

    /**
     * Path to the external run-properties file.
     * Injected from application.properties key: runprops.path
     * Can be overridden at launch:
     *   --runprops.path=/your/path/GSNOW_RunProperties_CFLNT.properties
     */
    @Value("${runprops.path}")
    private String path;

    private final Map<String, String> values = new HashMap<>();

    @PostConstruct
    public void load() throws Exception {
        if (path == null) {
            throw new IllegalArgumentException("System property 'params.propFileName' not provided");
        }

        String fileName = Paths.get(path).getFileName().toString();
        log.info("CONSUMER PROPERTIES being used is {}", fileName);

        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;

            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;

                int idx = line.indexOf('=');
                if (idx < 0) continue;

                String key = line.substring(0, idx).trim();
                String val = line.substring(idx + 1).trim();
                values.put(key, val);
            }
        }
    }

    public String get(String key) {
        return values.get(key);
    }

    public String getOrDefault(String key, String def) {
        return values.getOrDefault(key, def);
    }
}
