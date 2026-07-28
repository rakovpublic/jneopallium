package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmergencySafeModeTest {
    @Test
    void entersEmergencySafeModeAndPreservesTaskState() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("emergency_safe_mode");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "emergencySafeMode"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "movementStopped"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "taskStatePreserved"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "emergencyReport"));
    }
}
