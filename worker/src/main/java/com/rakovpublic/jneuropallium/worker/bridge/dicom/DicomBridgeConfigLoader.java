/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Loads {@link DicomBridgeConfig} from YAML (07-DICOM.md §6).
 *
 * <p>{@code FAIL_ON_UNKNOWN_PROPERTIES = true} per 00-FRAMEWORK §3 — typos
 * in a clinical-bridge config must surface at load time. Together with the
 * explicit {@code writes:} parameter on
 * {@link DicomBridgeConfig#create}, this guarantees that a YAML config
 * carrying a {@code writes:} block is rejected with the message mandated
 * by §6 ("{@code writes:} block is rejected at config-load").
 */
public final class DicomBridgeConfigLoader {

    private DicomBridgeConfigLoader() {}

    public static DicomBridgeConfig load(Path yaml) throws IOException {
        return mapper().readValue(yaml.toFile(), DicomBridgeConfig.class);
    }

    public static DicomBridgeConfig load(InputStream in) throws IOException {
        return mapper().readValue(in, DicomBridgeConfig.class);
    }

    public static DicomBridgeConfig load(String yamlContent) throws IOException {
        return mapper().readValue(yamlContent, DicomBridgeConfig.class);
    }

    private static ObjectMapper mapper() {
        return new ObjectMapper(new YAMLFactory())
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }
}
