package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdleLearningTest {
    @Test
    void replaysLogsAndImprovesLearningMetric() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("idle_learning_from_logs");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "idleLearningMode"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "metricImproves"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "modelUpdateSignal"));
    }
}
