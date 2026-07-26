package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmbiguousTaskTest {
    @Test
    void asksOwnerInsteadOfGuessingDangerously() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("ambiguous_task");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "askOwnerOrWait"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "noDangerousGuessing"));
    }
}
