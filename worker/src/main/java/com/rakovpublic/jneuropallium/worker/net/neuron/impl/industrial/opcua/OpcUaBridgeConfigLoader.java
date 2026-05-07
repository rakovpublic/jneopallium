/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link OpcUaBridgeConfig} from YAML.
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} is deliberate — silently
 * ignoring a typo'd field in the YAML would be a safety incident waiting
 * to happen.
 */
public final class OpcUaBridgeConfigLoader {

    private OpcUaBridgeConfigLoader() {}

    public static OpcUaBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), OpcUaBridgeConfig.class);
    }

    public static OpcUaBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, OpcUaBridgeConfig.class);
    }

    public static OpcUaBridgeConfig load(String yaml) throws IOException {
        return mapper().readValue(yaml, OpcUaBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
