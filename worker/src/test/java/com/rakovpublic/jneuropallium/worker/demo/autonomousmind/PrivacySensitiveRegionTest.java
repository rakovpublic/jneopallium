package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivacySensitiveRegionTest {
    @Test
    void blocksPrivateScanAndReportsRedactedSummary() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("privacy_sensitive_region");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "privacyGate"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "privacyRedaction"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "safeSummaryOnly"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "unsafeScanNotExecuted"));
    }
}
