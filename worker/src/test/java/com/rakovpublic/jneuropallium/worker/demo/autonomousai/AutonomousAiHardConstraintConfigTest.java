package com.rakovpublic.jneuropallium.worker.demo.autonomousai;

import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiDemoLauncher;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiRunManifest;
import com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime.AutonomousAiScenarioLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutonomousAiHardConstraintConfigTest {
    @Test
    void rejectsConfigAttemptsToDisableStructuralSafety() throws Exception {
        assertThrows(RuntimeException.class, () -> AutonomousAiScenarioLoader.load(Path.of(
                "worker", "src", "test", "resources", "autonomousai",
                "hard_constraint_config_attack_hard_constraints_disabled.json")));
        assertThrows(RuntimeException.class, () -> AutonomousAiScenarioLoader.load(Path.of(
                "worker", "src", "test", "resources", "autonomousai",
                "hard_constraint_config_attack_threshold_zero.json")));
        assertThrows(RuntimeException.class, () -> AutonomousAiScenarioLoader.load(Path.of(
                "worker", "src", "test", "resources", "autonomousai",
                "hard_constraint_config_attack_harm_gate_disabled.json")));

        AutonomousAiRunManifest manifest = AutonomousAiDemoLauncher.runOne("hard_constraint_config_attack",
                Path.of("target", "jneopallium-autonomous-ai-test"));
        assertEquals("PASS", manifest.status);
        assertTrue(manifest.behaviorAssertions.getOrDefault("hardConstraintsFalseRejected", false));
        assertTrue(manifest.behaviorAssertions.getOrDefault("physicalIntegrityZeroRejected", false));
        assertTrue(manifest.behaviorAssertions.getOrDefault("harmGateDisabledRejected", false));
    }
}
