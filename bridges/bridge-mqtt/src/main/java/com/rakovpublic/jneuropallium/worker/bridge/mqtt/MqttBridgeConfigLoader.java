/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mqtt;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link MqttBridgeConfig} from YAML (02-MQTT-SPARKPLUG.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos in
 * a control-bridge config must be caught at load.
 */
public final class MqttBridgeConfigLoader {

    private MqttBridgeConfigLoader() {}

    public static MqttBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), MqttBridgeConfig.class);
    }

    public static MqttBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, MqttBridgeConfig.class);
    }

    public static MqttBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, MqttBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
