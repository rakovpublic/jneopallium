/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

public final class IndustrialFmiManifestWriter {
    private IndustrialFmiManifestWriter() {}

    public static void write(Path outputDir, IndustrialFmiScenario scenario, long run) throws IOException {
        Files.createDirectories(outputDir);
        String json = "{"
                + "\"run\":" + run + ","
                + "\"scenario\":\"" + escape(scenario.name()) + "\","
                + "\"generatedAt\":\"" + Instant.now() + "\","
                + "\"fastLoopSeconds\":0.1,"
                + "\"slowLoopSeconds\":1.0,"
                + "\"mqttCanActuate\":false,"
                + "\"opcUaAutonomousCapable\":true"
                + "}\n";
        Files.writeString(outputDir.resolve("manifest.json"), json, StandardCharsets.UTF_8);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
