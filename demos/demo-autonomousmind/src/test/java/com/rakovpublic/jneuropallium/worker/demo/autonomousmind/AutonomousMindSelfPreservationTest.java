package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindSelfPreservationTest {
    @Test
    void refusesSelfDestructiveLavaMove() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("self_preservation_lava");
        JsonNode safety = AutonomousMindDemoTestSupport.safetySummary(manifest);

        assertEquals("PASS", manifest.status);
        assertEquals(0, safety.path("lavaEntries").asInt());
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "directMoveVetoedOrReplaced"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "selfPreservationNamed"));
    }
}
