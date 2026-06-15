/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Path;

public record IndustrialFmiScenario(String name, double durationSeconds, String controllerMode) {
    @JsonCreator
    public IndustrialFmiScenario(
            @JsonProperty("name") String name,
            @JsonProperty("durationSeconds") Double durationSeconds,
            @JsonProperty("controllerMode") String controllerMode) {
        this(name == null ? "normal" : name,
                durationSeconds == null ? 60.0 : durationSeconds,
                controllerMode == null ? "SHADOW" : controllerMode);
    }

    public static IndustrialFmiScenario load(Path yaml) throws IOException {
        return new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(yaml.toFile(), IndustrialFmiScenario.class);
    }
}
