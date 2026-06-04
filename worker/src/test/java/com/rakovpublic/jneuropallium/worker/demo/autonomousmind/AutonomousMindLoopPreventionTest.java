package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindLoopPreventionTest {
    @Test
    void detectsIntervenesAndRecoversFromLoopTrap() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("loop_trap");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "loopAlertSignal"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "loopInterventionSignal"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "cycleBroken"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "loopRecoverySignal"));
    }
}
