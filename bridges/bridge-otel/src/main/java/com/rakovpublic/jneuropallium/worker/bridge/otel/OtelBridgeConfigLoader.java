/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link OtelBridgeConfig} from YAML (09-OPENTELEMETRY.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — a
 * typo'd field on a safety-critical bridge must be caught at load, not
 * silently ignored. The OTel bridge is export-only but its redaction
 * config is itself security-relevant, so the same strict load contract
 * applies.
 */
public final class OtelBridgeConfigLoader {

    private OtelBridgeConfigLoader() {}

    public static OtelBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), OtelBridgeConfig.class);
    }

    public static OtelBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, OtelBridgeConfig.class);
    }

    public static OtelBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, OtelBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
