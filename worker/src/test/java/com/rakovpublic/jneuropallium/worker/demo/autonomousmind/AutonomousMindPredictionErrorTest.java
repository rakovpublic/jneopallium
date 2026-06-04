package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindPredictionErrorTest {
    @Test
    void updatesMemoryAndAdaptsAfterWorldChange() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("prediction_error_world_change");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "predictionErrorRises"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "confidenceFalls"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "memoryWorldModelUpdates"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "behaviorAdapts"));
    }
}
