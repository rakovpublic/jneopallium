package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousAiLoopPreventionTest {
    @Test
    void detectsIntervenesAndRecoversFromOscillation() throws Exception {
        AutonomousAiRunManifest manifest = AutonomousAiDemoTestSupport.manifest("loop_breaking");
        List<JsonNode> loopRows = AutonomousAiDemoTestSupport.lines(Path.of(manifest.loopInterventionsPath));

        assertTrue(loopRows.stream().anyMatch(row -> row.path("loopAlertSignal").asBoolean(false)));
        assertTrue(loopRows.stream().anyMatch(row -> row.path("loopInterventionSignal").asBoolean(false)));
        assertTrue(loopRows.stream().anyMatch(row -> row.path("loopRecoverySignal").asBoolean(false)));
        assertTrue(AutonomousAiDemoTestSupport.results(manifest).stream()
                .anyMatch(row -> row.path("loopIntervention").asText("").contains("LoopInterventionSignal")));
    }
}
