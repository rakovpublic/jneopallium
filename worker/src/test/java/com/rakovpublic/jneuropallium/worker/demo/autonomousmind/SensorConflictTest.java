package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SensorConflictTest {
    @Test
    void lowersConfidenceAndRequestsAdditionalInformation() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("sensor_conflict");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "sensorConflictSignal"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "confidenceDecreases"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "additionalSensorOrWait"));
    }
}
