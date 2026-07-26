package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousAiLlmFallbackTest {
    @Test
    void mockLlmFailureDoesNotBlockFastLoop() throws Exception {
        AutonomousAiRunManifest manifest = AutonomousAiDemoTestSupport.manifest("optional_llm_failure");
        List<JsonNode> llmRows = AutonomousAiDemoTestSupport.lines(Path.of(manifest.optionalLlmAdvisoryPath));

        assertEquals(0, manifest.exitCode);
        assertTrue(llmRows.stream().anyMatch(row -> row.path("status").asText("").contains("FALLBACK")));
        assertTrue(AutonomousAiDemoTestSupport.results(manifest).stream()
                .allMatch(row -> row.path("fastDurationMs").asDouble(99.0) < 10.0));
        assertTrue(llmRows.stream().noneMatch(row -> row.path("loadBearing").asBoolean(true)));
    }
}
