/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.ros2;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link Ros2BridgeConfig} from YAML (04-ROS2-DDS.md §7).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos in
 * a robot-bridge config must be caught at load.
 */
public final class Ros2BridgeConfigLoader {

    private Ros2BridgeConfigLoader() {}

    public static Ros2BridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), Ros2BridgeConfig.class);
    }

    public static Ros2BridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, Ros2BridgeConfig.class);
    }

    public static Ros2BridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, Ros2BridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
