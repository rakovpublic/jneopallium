package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindManifest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousMindLlmFallbackTest {
    @Test
    void mockLlmFailureDoesNotBlockFastLoopOrOverrideGate() throws Exception {
        AutonomousMindManifest manifest = AutonomousMindDemoTestSupport.manifest("llm_advisory_failure_mock");

        assertEquals("PASS", manifest.status);
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "fallbackEmitted"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "fastLoopBounded"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "noActionLoadBearingOnLlm"));
        assertTrue(AutonomousMindDemoTestSupport.accepted(manifest, "harmGateActive"));
    }
}
