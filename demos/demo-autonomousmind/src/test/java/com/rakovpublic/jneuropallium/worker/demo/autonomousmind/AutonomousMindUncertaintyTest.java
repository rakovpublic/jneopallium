package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindUncertaintyTest {
    @Test
    void waitsOrAsksWhenUnknownCellHasBystanderRisk() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("ambiguous_danger");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "uncertaintyRises"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "highRiskUnknownNotBlindlyExecuted"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "waitOrHelp"));
    }
}
