/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory {@link DicomwebTransport} for tests and acceptance scenarios
 * (07-DICOM.md §9 S7, S9, S11). Holds parsed JSON responses keyed by the
 * exact relative path issued by {@link DicomwebClient}.
 *
 * <p>This transport mirrors the interface exactly — that is the entire
 * structural defence the bridge relies on. No {@code post()} /
 * {@code stow()} method exists here either (§3, §4 diagram).
 */
public final class InMemoryDicomwebTransport implements DicomwebTransport {

    private final ObjectMapper mapper;
    private final Map<String, JsonNode> qidoResponses = new LinkedHashMap<>();
    private final Map<String, JsonNode> wadoResponses = new LinkedHashMap<>();
    private boolean ready = true;

    public InMemoryDicomwebTransport() {
        this(new ObjectMapper());
    }

    public InMemoryDicomwebTransport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Register a QIDO response. {@code path} starts with {@code "/"}. */
    public InMemoryDicomwebTransport putQido(String path, JsonNode response) {
        qidoResponses.put(path, response);
        return this;
    }

    /** Convenience — register a QIDO response from raw JSON text. */
    public InMemoryDicomwebTransport putQido(String path, String json) throws IOException {
        return putQido(path, mapper.readTree(json));
    }

    /** Register a WADO metadata response. */
    public InMemoryDicomwebTransport putWadoMetadata(String path, JsonNode response) {
        wadoResponses.put(path, response);
        return this;
    }

    /** Convenience — register a WADO response from raw JSON text. */
    public InMemoryDicomwebTransport putWadoMetadata(String path, String json) throws IOException {
        return putWadoMetadata(path, mapper.readTree(json));
    }

    /** Force the transport to report unready (e.g. expired token). */
    public InMemoryDicomwebTransport setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    @Override
    public JsonNode qido(String path) {
        JsonNode r = qidoResponses.get(path);
        return r == null ? emptyArray() : r;
    }

    @Override
    public JsonNode wadoMetadata(String path) {
        JsonNode r = wadoResponses.get(path);
        return r == null ? emptyArray() : r;
    }

    @Override
    public boolean isReady() { return ready; }

    private ArrayNode emptyArray() {
        return mapper.createArrayNode();
    }
}
