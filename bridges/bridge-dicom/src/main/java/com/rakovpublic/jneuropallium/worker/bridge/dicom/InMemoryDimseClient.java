/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory {@link DimseClient} for tests (07-DICOM.md §9 S8). Behaves
 * like a tiny PACS that answers C-FIND with a preloaded study list and
 * C-MOVE / C-GET with the preloaded SR instances.
 *
 * <p>Mirrors the read-only interface exactly — no {@code cStore()} method
 * is even possible to call through this transport.
 */
public final class InMemoryDimseClient implements DimseClient {

    private final List<JsonNode> findResponse = new ArrayList<>();
    private final List<JsonNode> moveResponse = new ArrayList<>();
    private boolean ready = true;

    public InMemoryDimseClient addStudyMatch(JsonNode study) {
        findResponse.add(study);
        return this;
    }

    public InMemoryDimseClient addSrInstance(JsonNode srInstance) {
        moveResponse.add(srInstance);
        return this;
    }

    public InMemoryDimseClient setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    @Override
    public List<JsonNode> cFind(JsonNode queryAttributes) {
        return List.copyOf(findResponse);
    }

    @Override
    public List<JsonNode> cMove(JsonNode queryAttributes) {
        return List.copyOf(moveResponse);
    }

    @Override
    public List<JsonNode> cGet(JsonNode queryAttributes) {
        return List.copyOf(moveResponse);
    }

    @Override
    public boolean isReady() { return ready; }
}
