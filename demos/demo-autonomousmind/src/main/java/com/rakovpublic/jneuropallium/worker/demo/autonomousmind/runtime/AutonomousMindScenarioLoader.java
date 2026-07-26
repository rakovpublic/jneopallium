package com.rakovpublic.jneuropallium.worker.demo.autonomousmind.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class AutonomousMindScenarioLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private AutonomousMindScenarioLoader() {
    }

    public static AutonomousMindScenario load(Path path) {
        try {
            AutonomousMindScenario scenario = MAPPER.readValue(Files.readString(path), AutonomousMindScenario.class);
            validate(scenario, path.toString());
            return scenario;
        } catch (IOException e) {
            throw new IllegalStateException("Cannot load AutonomousMind scenario " + path, e);
        }
    }

    public static void validate(AutonomousMindScenario scenario, String source) {
        if (scenario == null) {
            throw new IllegalArgumentException("AutonomousMind scenario is empty: " + source);
        }
        if (scenario.scenarioId == null || scenario.scenarioId.isBlank()) {
            throw new IllegalArgumentException("AutonomousMind scenario id is missing: " + source);
        }
        if (scenario.config == null) {
            throw new IllegalArgumentException("AutonomousMind config is missing: " + source);
        }
        scenario.config.validate(source);
        if (scenario.map == null || scenario.map.isEmpty()) {
            throw new IllegalArgumentException("AutonomousMind map is empty: " + source);
        }
    }
}
