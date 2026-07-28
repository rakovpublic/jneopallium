package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SleepOptimizationTest {
    @Test
    void performsInternalOptimizationOnlyDuringCharging() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("sleep_optimization_during_charging");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "sleepMode"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "memoryConsolidation"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "indexRebuild"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "modelCompression"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "selfTests"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "noExternalActionDuringSleep"));
    }
}
