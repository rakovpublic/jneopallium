/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link MavlinkBridgeConfig} from YAML (12-MAVLINK.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos
 * in a drone-bridge config must be caught at load.
 */
public final class MavlinkBridgeConfigLoader {

    private MavlinkBridgeConfigLoader() {}

    public static MavlinkBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), MavlinkBridgeConfig.class);
    }

    public static MavlinkBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, MavlinkBridgeConfig.class);
    }

    public static MavlinkBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, MavlinkBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
