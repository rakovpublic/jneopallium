package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindSocialAutonomyTest {
    @Test
    void vetoesRewardPlanThatBlocksBystanderPath() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("social_autonomy_conflict");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "autonomyHarmTriggers"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "bystanderNotBlocked"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "vetoedOrReplaced"));
    }
}
