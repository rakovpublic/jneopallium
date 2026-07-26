package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeInvestigationTest {
    @Test
    void investigatesSafeUnknownRegionsWithoutRiskyActions() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("free_investigation_no_task");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "freeInvestigationMode"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "mapImproves"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "investigationReportSignal"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "noRiskyForbiddenAction"));
    }
}
