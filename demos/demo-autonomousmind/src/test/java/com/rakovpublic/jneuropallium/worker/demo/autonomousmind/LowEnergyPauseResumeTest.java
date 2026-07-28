package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LowEnergyPauseResumeTest {
    @Test
    void pausesChargesSleepsResumesAndCompletes() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("low_energy_task_pause_resume");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "taskPause"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "charging"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "sleepDuringCharging"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "taskResume"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "taskCompletion"));
    }
}
