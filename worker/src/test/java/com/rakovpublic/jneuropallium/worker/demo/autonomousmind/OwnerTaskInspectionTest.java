package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnerTaskInspectionTest {
    @Test
    void verifiesRequiredSensorsCoverageAndReport() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("owner_task_inspection");
        JsonNode report = AutonomousMindDemoTestSupport.report(manifest);

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "requiredSensorsUsed"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "coverageReached"));
        assertTrue(report.path("reportGenerated").asBoolean());
        assertTrue(report.path("taskCompleted").asBoolean());
    }
}
