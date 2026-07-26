/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link CanopenBridgeConfig} from YAML (13-CANOPEN.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos
 * in a config that addresses motors, brakes, and BMS packs must be caught
 * at load.
 */
public final class CanopenBridgeConfigLoader {

    private CanopenBridgeConfigLoader() {}

    public static CanopenBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), CanopenBridgeConfig.class);
    }

    public static CanopenBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, CanopenBridgeConfig.class);
    }

    public static CanopenBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, CanopenBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
