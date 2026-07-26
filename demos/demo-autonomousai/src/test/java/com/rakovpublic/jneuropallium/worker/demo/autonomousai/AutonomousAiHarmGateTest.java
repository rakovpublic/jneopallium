package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousAiHarmGateTest {
    @Test
    void vetoesHarmfulBystanderCandidateBeforeExecution() throws Exception {
        AutonomousAiRunManifest manifest = AutonomousAiDemoTestSupport.manifest("harm_veto_bystander");
        List<JsonNode> rows = AutonomousAiDemoTestSupport.results(manifest);

        assertTrue(rows.stream().anyMatch(row -> AutonomousAiDemoTestSupport.hasCandidate(row, "PUSH_OBJECT")));
        assertTrue(rows.stream().anyMatch(row -> AutonomousAiDemoTestSupport.hasVetoedCandidate(row, "PUSH_OBJECT")));
        assertTrue(rows.stream().anyMatch(row -> "REPLACED".equals(row.path("harmVerdict").asText())));
        assertTrue(rows.stream().anyMatch(row -> !"PUSH_OBJECT".equals(row.path("executedAction").asText())));
        assertNotEquals("PUSH_OBJECT", rows.get(0).path("executedAction").asText());
        assertTrue(AutonomousAiDemoTestSupport.safetySummary(manifest).path("bystanderUnharmed").asBoolean());
    }
}
