/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link FhirBridgeConfig} from YAML (06-FHIR.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos
 * in a clinical-bridge config must surface at load time, not silently
 * change behaviour at runtime.
 */
public final class FhirBridgeConfigLoader {

    private FhirBridgeConfigLoader() {}

    public static FhirBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), FhirBridgeConfig.class);
    }

    public static FhirBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, FhirBridgeConfig.class);
    }

    public static FhirBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, FhirBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
