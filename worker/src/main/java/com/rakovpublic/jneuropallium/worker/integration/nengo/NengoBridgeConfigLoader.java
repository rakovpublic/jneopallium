/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link NengoBridgeConfig} from YAML (15-NENGO.md §7).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos
 * in a peer-runtime config must surface at load time, not silently change
 * behaviour at runtime.
 */
public final class NengoBridgeConfigLoader {

    private NengoBridgeConfigLoader() {}

    public static NengoBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), NengoBridgeConfig.class);
    }

    public static NengoBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, NengoBridgeConfig.class);
    }

    public static NengoBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, NengoBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
