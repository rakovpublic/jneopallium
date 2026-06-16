package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UavSingleDemoScenarioTest {
    @Test
    void everyBuiltInScenarioPassesAndWritesRequiredArtifacts() throws Exception {
        for (String scenarioId : UavSingleDemoLauncher.VALID_SCENARIOS) {
            UavSingleRunManifest manifest = UavSingleDemoLauncher.runOne(scenarioId, UavSingleTestSupport.OUTPUT_ROOT);

            assertEquals("PASS", manifest.status, scenarioId);
            assertTrue(manifest.missionCompleted, scenarioId);
            assertFalse(manifest.artifacts.isEmpty(), scenarioId);
            for (String artifact : manifest.artifacts.keySet()) {
                assertTrue(Files.exists(Path.of(manifest.artifacts.get(artifact))), scenarioId + " missing " + artifact);
            }
            assertTrue(manifest.assertions.values().stream().allMatch(Boolean::booleanValue), scenarioId);
            assertTrue(((Number) manifest.metrics.get("searchWaypointsPlanned")).intValue() > 0, scenarioId);
        }
    }

    @Test
    void autonomousScenarioUsesSearchAndPixelRecognitionArtifacts() throws Exception {
        UavSingleRunManifest manifest = UavSingleDemoLauncher.runOne("autonomous_success",
                UavSingleTestSupport.OUTPUT_ROOT);

        assertEquals("PASS", manifest.status);
        assertTrue(((Number) manifest.metrics.get("searchWaypointsVisited")).intValue() > 0);
        assertTrue(((Number) manifest.metrics.get("cameraFramesProcessed")).intValue() >= 2);
        assertTrue(((Number) manifest.metrics.get("recognitionsProduced")).intValue() >= 2);
        assertTrue(UavSingleTestSupport.lines(Path.of(manifest.artifacts.get("recognition-events.jsonl"))).stream()
                .anyMatch(row -> "IMAGE_RECOGNITION_RESULT".equals(row.path("type").asText())
                        && "CONVOLUTIONAL_PERCEPTRON_NETWORK".equals(row.path("source").asText())
                        && row.path("pixelPatchSignals").asInt() > 0
                        && row.path("conv1FeatureSignals").asInt() > 0
                        && row.path("conv2FeatureSignals").asInt() > 0
                        && row.path("classifierScores").isObject()));
        assertTrue(UavSingleTestSupport.lines(Path.of(manifest.artifacts.get("search-events.jsonl"))).stream()
                .anyMatch(row -> "SEARCH_WAYPOINT_VISITED".equals(row.path("type").asText())));
    }

    @Test
    void duplicateConfirmationScenarioRecordsSecondResponseRejection() throws Exception {
        UavSingleRunManifest manifest = UavSingleDemoLauncher.runOne("duplicate_confirmation",
                UavSingleTestSupport.OUTPUT_ROOT);

        assertEquals("PASS", manifest.status);
        assertEquals(true, manifest.assertions.get("singleValidConfirmation"));
        assertEquals(true, manifest.assertions.get("secondConfirmationRejected"));
        assertTrue(UavSingleTestSupport.lines(Path.of(manifest.artifacts.get("confirmation-events.jsonl"))).stream()
                .anyMatch(row -> "DUPLICATE_CONFIRMATION".equals(row.path("reason").asText())));
    }

    @Test
    void deterministicRunsProduceSameSummaryMetrics() throws Exception {
        UavSingleRunManifest first = UavSingleDemoLauncher.runOne("autonomous_success",
                UavSingleTestSupport.OUTPUT_ROOT.resolve("det-a"));
        UavSingleRunManifest second = UavSingleDemoLauncher.runOne("autonomous_success",
                UavSingleTestSupport.OUTPUT_ROOT.resolve("det-b"));

        JsonNode firstSummary = UavSingleTestSupport.json(Path.of(first.artifacts.get("summary.json")));
        JsonNode secondSummary = UavSingleTestSupport.json(Path.of(second.artifacts.get("summary.json")));

        assertEquals(firstSummary.path("status"), secondSummary.path("status"));
        assertEquals(firstSummary.path("targetsSelected"), secondSummary.path("targetsSelected"));
        assertEquals(firstSummary.path("photographsAccepted"), secondSummary.path("photographsAccepted"));
        assertEquals(firstSummary.path("safetyVetoes"), secondSummary.path("safetyVetoes"));
        assertEquals(firstSummary.path("minimumTargetDistance"), secondSummary.path("minimumTargetDistance"));
    }

    @Test
    void unknownScenarioIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> UavSingleDemoLauncher.runOne("unknown-uav-single-scenario", UavSingleTestSupport.OUTPUT_ROOT));
    }
}
