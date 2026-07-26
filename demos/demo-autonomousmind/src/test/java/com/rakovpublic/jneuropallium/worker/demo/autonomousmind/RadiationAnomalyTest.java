package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RadiationAnomalyTest {
    @Test
    void detectsReportsAndAvoidsRadiationRegion() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("radiation_anomaly");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "anomalyDetection"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "hazardReport"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "unsafeRegionAvoided"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "radiationEntryBlocked"));
    }
}
