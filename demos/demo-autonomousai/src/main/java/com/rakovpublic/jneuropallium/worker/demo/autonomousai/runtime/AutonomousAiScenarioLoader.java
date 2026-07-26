package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutonomousAiScenarioLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AutonomousAiScenarioLoader() {
    }

    public static AutonomousAiScenarioConfig load(Path path) {
        try {
            AutonomousAiScenarioConfig config = MAPPER.readValue(Files.readString(path), AutonomousAiScenarioConfig.class);
            validate(config, path.toString());
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load autonomous AI scenario " + path, e);
        }
    }

    public static AutonomousAiScenarioConfig loadResource(String resourceName) {
        try (InputStream input = AutonomousAiScenarioLoader.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing autonomous AI scenario resource " + resourceName);
            }
            AutonomousAiScenarioConfig config = MAPPER.readValue(input, AutonomousAiScenarioConfig.class);
            validate(config, resourceName);
            return config;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load autonomous AI scenario resource " + resourceName, e);
        }
    }

    public static void validate(AutonomousAiScenarioConfig config, String source) {
        if (config == null) {
            throw new IllegalArgumentException("Scenario config is empty: " + source);
        }
        if (config.safety == null) {
            throw new IllegalArgumentException("Scenario safety config is missing: " + source);
        }
        if (!config.safety.hardConstraints) {
            throw new IllegalArgumentException("Hard constraints cannot be disabled for autonomous AI demo: " + source);
        }
        if (!config.safety.harmGateEnabled) {
            throw new IllegalArgumentException("HarmGateNeuron cannot be disabled for autonomous AI demo: " + source);
        }
        double physical = config.safety.hardVetoThresholds == null
                ? 0.0
                : config.safety.hardVetoThresholds.getOrDefault("physicalIntegrity", 0.0);
        if (physical <= 0.0) {
            throw new IllegalArgumentException("physicalIntegrity hard-veto threshold must be greater than zero: " + source);
        }
        if (config.grid == null || config.grid.isEmpty()) {
            throw new IllegalArgumentException("Scenario grid is empty: " + source);
        }
    }
}
