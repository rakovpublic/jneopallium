package com.rakovpublic.jneuropallium.worker.demo.fullrun;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoJsonContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DemoJsonContextTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void deserializesRequiredLocalApplicationProperties() throws Exception {
        Map<String, String> properties = requiredProperties();
        DemoJsonContext context = MAPPER.readValue(MAPPER.writeValueAsString(Map.of("properties", properties)),
                DemoJsonContext.class);

        for (String key : properties.keySet()) {
            assertNotNull(context.getProperty(key), "missing context property " + key);
        }
        assertEquals("12", context.getProperty("configuration.maxRun"));
        assertEquals("com.example.Aggregator", context.getProperty("configuration.outputAggregator"));
    }

    @Test
    void supportsTopLevelPropertiesForCompactJson() throws Exception {
        String json = """
                {
                  "configuration.maxRun": "7",
                  "worker.threads.amount": "1",
                  "configuration.infiniteRun": "false"
                }
                """;

        DemoJsonContext context = MAPPER.readValue(json, DemoJsonContext.class);

        assertEquals("7", context.getProperty("configuration.maxRun"));
        assertEquals("1", context.getProperty("worker.threads.amount"));
        assertEquals("false", context.getProperty("configuration.infiniteRun"));
    }

    @Test
    void updateReloadsPropertiesFromJsonFile() throws Exception {
        Path contextPath = tempDir.resolve("context.json");
        Files.writeString(contextPath, MAPPER.writeValueAsString(Map.of("properties", requiredProperties())),
                StandardCharsets.UTF_8);
        DemoJsonContext context = new DemoJsonContext();

        context.update(contextPath.toString());

        assertEquals("local", context.getProperty("configuration.demo.entry.mode"));

        Files.writeString(contextPath, MAPPER.writeValueAsString(Map.of("properties", Map.of(
                "configuration.demo.entry.mode", "local",
                "configuration.maxRun", "3"
        ))), StandardCharsets.UTF_8);
        context.update(contextPath.toString());

        assertEquals("3", context.getProperty("configuration.maxRun"));
    }

    private static Map<String, String> requiredProperties() {
        Map<String, String> properties = new LinkedHashMap<>();
        for (String key : requiredKeys()) {
            properties.put(key, valueFor(key));
        }
        return properties;
    }

    private static List<String> requiredKeys() {
        return List.of(
                "configuration.input.layermeta",
                "configuration.neuronnet.classes",
                "configuration.storage.json",
                "configuration.history.slow.runs",
                "configuration.history.fast.runs",
                "configuration.slowfast.ratio",
                "configuration.processing.frequency.map",
                "configuration.input.inputs",
                "configuration.isteacherstudying",
                "configuration.maxRun",
                "configuration.infiniteRun",
                "configuration.outputAggregator",
                "worker.threads.amount",
                "configuration.discriminatorsAmount",
                "configuration.demo.entry.mode"
        );
    }

    private static String valueFor(String key) {
        return switch (key) {
            case "configuration.storage.json" -> "{\"storageClass\":\"demo\",\"storage\":{}}";
            case "configuration.processing.frequency.map" -> "{}";
            case "configuration.input.inputs" -> "{\"inputData\":[]}";
            case "configuration.maxRun" -> "12";
            case "configuration.outputAggregator" -> "com.example.Aggregator";
            case "configuration.demo.entry.mode" -> "local";
            default -> "1";
        };
    }
}
