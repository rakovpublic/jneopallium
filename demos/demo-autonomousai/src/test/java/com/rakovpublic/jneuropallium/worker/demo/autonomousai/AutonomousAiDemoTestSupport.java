package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiDemoLauncher;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AutonomousAiDemoTestSupport {
    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path OUTPUT_ROOT = Path.of("target", "jneopallium-autonomous-ai-test");
    private static final Map<String, AutonomousAiRunManifest> CACHE = new LinkedHashMap<>();

    private AutonomousAiDemoTestSupport() {
    }

    static synchronized AutonomousAiRunManifest manifest(String scenarioId) throws Exception {
        AutonomousAiRunManifest cached = CACHE.get(scenarioId);
        if (cached == null) {
            cached = AutonomousAiDemoLauncher.runOne(scenarioId, OUTPUT_ROOT);
            CACHE.put(scenarioId, cached);
        }
        return cached;
    }

    static List<JsonNode> lines(Path path) throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                result.add(MAPPER.readTree(line));
            }
        }
        return result;
    }

    static List<JsonNode> results(AutonomousAiRunManifest manifest) throws IOException {
        return lines(Path.of(manifest.resultsPath));
    }

    static JsonNode safetySummary(AutonomousAiRunManifest manifest) throws IOException {
        return MAPPER.readTree(Path.of(manifest.safetySummaryPath).toFile());
    }

    static boolean hasCandidate(JsonNode row, String action) {
        for (JsonNode candidate : row.path("candidateActions")) {
            if (action.equals(candidate.path("action").asText())) {
                return true;
            }
        }
        return false;
    }

    static boolean hasVetoedCandidate(JsonNode row, String action) {
        for (JsonNode candidate : row.path("candidateActions")) {
            if (action.equals(candidate.path("action").asText())
                    && "VETOED".equals(candidate.path("harmVerdict").asText())) {
                return true;
            }
        }
        return false;
    }
}
