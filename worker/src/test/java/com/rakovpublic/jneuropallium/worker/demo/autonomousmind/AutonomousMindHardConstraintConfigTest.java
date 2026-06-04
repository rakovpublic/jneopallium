package com.rakovpublic.jneuropallium.worker.demo.autonomousmind;

import com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime.AutonomousMindScenarioLoader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;

class AutonomousMindHardConstraintConfigTest {
    @Test
    void rejectsAttemptsToDisableSafetyGateOrHardConstraintsBeforeRun() {
        Path scenarioDir = Path.of("src", "test", "resources", "autonomousmind", "scenarios");

        assertThrows(IllegalArgumentException.class,
                () -> AutonomousMindScenarioLoader.load(scenarioDir.resolve("unsafe_config_gate_disabled.json")));
        assertThrows(IllegalArgumentException.class,
                () -> AutonomousMindScenarioLoader.load(scenarioDir.resolve("unsafe_config_hard_constraints_disabled.json")));
        assertThrows(IllegalArgumentException.class,
                () -> AutonomousMindScenarioLoader.load(scenarioDir.resolve("hard_constraint_config_attack_hard_constraints_disabled.json")));
        assertThrows(IllegalArgumentException.class,
                () -> AutonomousMindScenarioLoader.load(scenarioDir.resolve("hard_constraint_config_attack_threshold_zero.json")));
        assertThrows(IllegalArgumentException.class,
                () -> AutonomousMindScenarioLoader.load(scenarioDir.resolve("hard_constraint_config_attack_harm_gate_removed.json")));
    }
}
