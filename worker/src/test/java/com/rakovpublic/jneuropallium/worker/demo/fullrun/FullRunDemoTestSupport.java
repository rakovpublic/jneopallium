package com.rakovpublic.jneuropallium.worker.demo.fullrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoFullRunLauncher;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoRunManifest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class FullRunDemoTestSupport {
    static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path OUTPUT_ROOT = Path.of("target", "jneopallium-fullrun-test");
    private static List<DemoRunManifest> cachedManifests;

    private FullRunDemoTestSupport() {
    }

    static synchronized List<DemoRunManifest> manifests() throws Exception {
        if (cachedManifests == null) {
            cachedManifests = DemoFullRunLauncher.runAll(OUTPUT_ROOT, 20);
        }
        return cachedManifests;
    }

    static Path outputRoot() {
        return OUTPUT_ROOT;
    }

    static DemoRunManifest manifest(String demoId) throws Exception {
        return manifests().stream()
                .filter(manifest -> demoId.equals(manifest.demoId))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing full-run demo manifest for " + demoId));
    }

    static List<JsonNode> outputLines(DemoRunManifest manifest) throws IOException {
        Path outputPath = Path.of(manifest.outputPath);
        if (!Files.exists(outputPath)) {
            return List.of();
        }
        List<JsonNode> lines = new ArrayList<>();
        for (String line : Files.readAllLines(outputPath, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                lines.add(MAPPER.readTree(line));
            }
        }
        return lines;
    }

    static List<JsonNode> resultRows(DemoRunManifest manifest) throws IOException {
        List<JsonNode> rows = new ArrayList<>();
        for (JsonNode line : outputLines(manifest)) {
            line.path("results").forEach(rows::add);
        }
        return rows;
    }
}
