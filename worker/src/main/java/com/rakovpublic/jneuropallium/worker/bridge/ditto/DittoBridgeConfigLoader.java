/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link DittoBridgeConfig} from YAML (10-DITTO.md §5).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — a typo
 * in a control-bridge config must be caught at load.
 */
public final class DittoBridgeConfigLoader {

    private DittoBridgeConfigLoader() {}

    public static DittoBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), DittoBridgeConfig.class);
    }

    public static DittoBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, DittoBridgeConfig.class);
    }

    public static DittoBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, DittoBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
