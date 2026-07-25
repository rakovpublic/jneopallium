/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link LslBridgeConfig} from YAML (05-LSL.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — a typo
 * in a config that drives EEG / physiological recording must be caught at
 * load, not silently ignored.
 */
public final class LslBridgeConfigLoader {

    private LslBridgeConfigLoader() {}

    public static LslBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), LslBridgeConfig.class);
    }

    public static LslBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, LslBridgeConfig.class);
    }

    public static LslBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, LslBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
