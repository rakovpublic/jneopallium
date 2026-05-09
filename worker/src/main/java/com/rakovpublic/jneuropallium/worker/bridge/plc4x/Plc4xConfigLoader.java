/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link Plc4xConfig} from YAML (01-PLC4X.md §5).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — a
 * typo'd field on a safety-critical bridge must be caught at load, not
 * silently ignored.
 */
public final class Plc4xConfigLoader {

    private Plc4xConfigLoader() {}

    public static Plc4xConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), Plc4xConfig.class);
    }

    public static Plc4xConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, Plc4xConfig.class);
    }

    public static Plc4xConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, Plc4xConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .registerModule(new SimpleModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
