/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link FmiBridgeConfig} from YAML (03-FMI-FMU.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} is deliberate — a typo'd
 * field in the safety-critical YAML must be caught at load time, not
 * silently ignored.
 */
public final class FmiBridgeConfigLoader {

    private FmiBridgeConfigLoader() {}

    public static FmiBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), FmiBridgeConfig.class);
    }

    public static FmiBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, FmiBridgeConfig.class);
    }

    public static FmiBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, FmiBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
