package com.rakovpublic.jneuropallium.worker.demo.fullrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime.DemoRunManifest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullRunDemoBehaviorTest {

    @Test
    void launcherBehaviorAssertionsAllPass() throws Exception {
        for (DemoRunManifest manifest : FullRunDemoTestSupport.manifests()) {
            assertFalse(manifest.behaviorAssertions.isEmpty(), manifest.demoId);
            for (Map.Entry<String, Boolean> assertion : manifest.behaviorAssertions.entrySet()) {
                assertTrue(assertion.getValue(), manifest.demoId + " failed " + assertion.getKey());
            }
        }
    }

    @Test
    void industrialControlProducesNormalFailSafeAndHeldCommands() throws Exception {
        DemoRunManifest manifest = FullRunDemoTestSupport.manifest("demo-01-industrial-control");
        List<JsonNode> rows = FullRunDemoTestSupport.resultRows(manifest);

        assertTrue(typeCount(rows, "VALVE_COMMAND") > 0);
        assertTrue(rows.stream().anyMatch(row -> "FAIL_SAFE_COMMAND".equals(row.path("resultType").asText())
                && row.path("tick").asInt(-1) >= 5 && row.path("tick").asInt(-1) <= 6));
        assertTrue(typeCount(rows, "HELD_COMMAND") > 0);
    }

    @Test
    void pumpFleetAdvisesOnlyTheDegradingAndOfflineAssets() throws Exception {
        DemoRunManifest manifest = FullRunDemoTestSupport.manifest("demo-02-pump-fleet-maintenance");

        assertTrue(metricAsDouble(manifest, "pump03MinRul") < metricAsDouble(manifest, "pump00MinRul"));
        assertTrue(manifest.behaviorAssertions.get("maintenanceAdvisoryEmitted"));
        assertTrue(manifest.behaviorAssertions.get("offlinePumpAdvisory"));
        assertTrue(manifest.behaviorAssertions.get("healthyPumpZeroMaintenanceAdvisory"));
    }

    @Test
    void droneMissionGuardAllowsVetoesAndRecommendsReturnHome() throws Exception {
        DemoRunManifest manifest = FullRunDemoTestSupport.manifest("demo-03-drone-mavlink-guard");
        List<JsonNode> rows = FullRunDemoTestSupport.resultRows(manifest);

        assertTrue(typeCount(rows, "COMMAND_ALLOWED") > 0);
        assertTrue(typeCount(rows, "COMMAND_VETO") > 0);
        assertTrue(typeCount(rows, "RETURN_TO_HOME_ADVISORY") > 0);
    }

    @Test
    void advisoryReadOnlyAndExportOnlyDemosDoNotEmitWriteLikeResults() throws Exception {
        assertTrue(FullRunDemoTestSupport.manifest("demo-04-clinical-fhir-advisory")
                .behaviorAssertions.get("noAutonomousOrderOrWrite"));
        assertTrue(FullRunDemoTestSupport.manifest("demo-05-dicom-readonly-context")
                .behaviorAssertions.get("noCommandOrWriteback"));
        assertTrue(FullRunDemoTestSupport.manifest("demo-06-cybersecurity-kafka-triage")
                .behaviorAssertions.get("advisoryOnly"));
        assertTrue(FullRunDemoTestSupport.manifest("demo-07-observability-otel-export")
                .behaviorAssertions.get("noWritebackControl"));
    }

    @Test
    void domainSpecificSyntheticScenariosAreVisibleInResults() throws Exception {
        assertEquals("ADVISORY",
                firstRow("demo-04-clinical-fhir-advisory").path("mode").asText());
        assertEquals("READ-ONLY",
                firstRow("demo-05-dicom-readonly-context").path("mode").asText());
        assertEquals("EXPORT-ONLY",
                firstRow("demo-07-observability-otel-export").path("mode").asText());

        assertTrue(FullRunDemoTestSupport.manifest("demo-06-cybersecurity-kafka-triage")
                .behaviorAssertions.get("attackScoreGreaterThanBenign"));
        assertTrue(FullRunDemoTestSupport.manifest("demo-08-adaptive-tutoring-lti")
                .behaviorAssertions.get("strugglingLearnerHint"));
        assertTrue(FullRunDemoTestSupport.manifest("demo-08-adaptive-tutoring-lti")
                .behaviorAssertions.get("strongLearnerHarderExercise"));
        assertTrue(FullRunDemoTestSupport.manifest("demo-09-nengo-interop")
                .behaviorAssertions.get("confidenceEmitted"));
    }

    private static JsonNode firstRow(String demoId) throws Exception {
        List<JsonNode> rows = FullRunDemoTestSupport.resultRows(FullRunDemoTestSupport.manifest(demoId));
        assertFalse(rows.isEmpty(), demoId);
        return rows.get(0);
    }

    private static long typeCount(List<JsonNode> rows, String resultType) {
        return rows.stream().filter(row -> resultType.equals(row.path("resultType").asText())).count();
    }

    private static double metricAsDouble(DemoRunManifest manifest, String metric) {
        Object value = manifest.metrics.get(metric);
        assertTrue(value instanceof Number, manifest.demoId + " missing numeric metric " + metric);
        return ((Number) value).doubleValue();
    }
}
