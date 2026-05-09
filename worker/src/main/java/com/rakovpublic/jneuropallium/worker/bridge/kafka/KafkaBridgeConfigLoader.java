/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.kafka;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link KafkaBridgeConfig} from YAML (08-KAFKA.md §5).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos in
 * a security-bridge config must be caught at load, not at runtime.
 */
public final class KafkaBridgeConfigLoader {

    private KafkaBridgeConfigLoader() {}

    public static KafkaBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), KafkaBridgeConfig.class);
    }

    public static KafkaBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, KafkaBridgeConfig.class);
    }

    public static KafkaBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, KafkaBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
