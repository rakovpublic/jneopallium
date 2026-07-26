package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class UavSingleScenarioLoader {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private UavSingleScenarioLoader() {
    }

    public static UavSingleScenario load(Path path) {
        try {
            UavSingleScenario scenario = MAPPER.readValue(path.toFile(), UavSingleScenario.class);
            scenario.validate();
            return scenario;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load UAV single scenario " + path, e);
        }
    }

    public static Path resolveScenarioPath(String scenarioId) {
        List<Path> candidates = List.of(
                Path.of("worker", "src", "test", "resources", "uavsingle", scenarioId + ".json"),
                Path.of("src", "test", "resources", "uavsingle", scenarioId + ".json"));
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return candidate.toAbsolutePath().normalize();
            }
        }
        try {
            var resource = UavSingleScenarioLoader.class.getClassLoader()
                    .getResource("uavsingle/" + scenarioId + ".json");
            if (resource != null && "file".equals(resource.getProtocol())) {
                return Path.of(resource.toURI()).toAbsolutePath().normalize();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Cannot resolve UAV single scenario " + scenarioId, e);
        }
        throw new IllegalArgumentException("Missing UAV single scenario " + scenarioId);
    }
}

