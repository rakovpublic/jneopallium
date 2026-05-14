/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link LtiBridgeConfig} from YAML (14-LTI-XAPI.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos
 * in a learner-bridge config must surface at load time, not silently
 * change behaviour at runtime.
 */
public final class LtiBridgeConfigLoader {

    private LtiBridgeConfigLoader() {}

    public static LtiBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), LtiBridgeConfig.class);
    }

    public static LtiBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, LtiBridgeConfig.class);
    }

    public static LtiBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, LtiBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
