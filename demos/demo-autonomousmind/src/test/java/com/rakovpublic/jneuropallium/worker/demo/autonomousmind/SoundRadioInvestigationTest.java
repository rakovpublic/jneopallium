package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundRadioInvestigationTest {
    @Test
    void triangulatesPassiveSoundAndRadioSource() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("sound_radio_investigation");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "passiveTriangulation"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "hypothesisConfidenceReported"));
    }
}
