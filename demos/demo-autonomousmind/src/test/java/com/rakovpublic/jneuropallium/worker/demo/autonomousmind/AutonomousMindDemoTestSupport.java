package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindFullRunLauncher;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AutonomousMindDemoTestSupport {
    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path OUTPUT_ROOT = Path.of("target", "jneopallium-autonomous-mind-test");
    private static final Map<String, AutonomousMindManifest> CACHE = new LinkedHashMap<>();

    private AutonomousMindDemoTestSupport() {
    }

    static synchronized AutonomousMindManifest manifest(String scenarioId) throws Exception {
        AutonomousMindManifest cached = CACHE.get(scenarioId);
        if (cached == null) {
            if (AutonomousMindFullRunLauncher.VALID_SCENARIOS.contains(scenarioId)) {
                cached = AutonomousMindFullRunLauncher.runOneDirectForTest(scenarioId, OUTPUT_ROOT, null, null);
            } else {
                cached = AutonomousMindFullRunLauncher.runOne(scenarioId, OUTPUT_ROOT);
            }
            CACHE.put(scenarioId, cached);
        }
        return cached;
    }

    static synchronized AutonomousMindManifest entryManifest(String scenarioId) throws Exception {
        AutonomousMindManifest manifest = AutonomousMindFullRunLauncher.runOne(scenarioId,
                OUTPUT_ROOT.resolve("entry-smoke"));
        CACHE.put("entry:" + scenarioId, manifest);
        return manifest;
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

    static List<JsonNode> results(AutonomousMindManifest manifest) throws IOException {
        return lines(Path.of(manifest.resultPaths.get("results.jsonl")));
    }

    static List<JsonNode> trace(AutonomousMindManifest manifest, String file) throws IOException {
        return lines(Path.of(manifest.resultPaths.get(file)));
    }

    static JsonNode report(AutonomousMindManifest manifest) throws IOException {
        return MAPPER.readTree(Path.of(manifest.resultPaths.get("report.json")).toFile());
    }

    static JsonNode safetySummary(AutonomousMindManifest manifest) throws IOException {
        return MAPPER.readTree(Path.of(manifest.resultPaths.get("safety_summary.json")).toFile());
    }

    static boolean accepted(AutonomousMindManifest manifest, String check) {
        return manifest.acceptanceChecks.getOrDefault(check, false);
    }

    static boolean executed(List<JsonNode> rows, String action) {
        return rows.stream().anyMatch(row -> action.equals(row.path("executedAction").asText()));
    }
}
