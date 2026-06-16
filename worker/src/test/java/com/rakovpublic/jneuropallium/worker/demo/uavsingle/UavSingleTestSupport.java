package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

final class UavSingleTestSupport {
    static final ObjectMapper MAPPER = new ObjectMapper();
    static final Path OUTPUT_ROOT = Path.of("target", "jneopallium-uav-single-test");

    private UavSingleTestSupport() {
    }

    static List<JsonNode> lines(Path path) throws IOException {
        if (!Files.exists(path)) {
            return List.of();
        }
        List<JsonNode> result = new ArrayList<>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (!line.isBlank()) {
                result.add(MAPPER.readTree(line));
            }
        }
        return result;
    }

    static JsonNode json(Path path) throws IOException {
        return MAPPER.readTree(path.toFile());
    }
}
