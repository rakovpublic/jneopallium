/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.List;

/**
 * Read-only DIMSE client seam (07-DICOM.md §4, §7).
 *
 * <p><b>Structurally read-only.</b> The interface exposes only the three
 * classical query/retrieve DIMSE primitives that the bridge needs —
 * C-FIND (study/series/instance query), C-MOVE (queued retrieval) and
 * C-GET (synchronous retrieval). There is intentionally no
 * {@code cStore()} / {@code cEcho()} write path; a code path that pushes a
 * DICOM instance to the PACS cannot exist within the bridge because the
 * seam to the wire does not provide one (07-DICOM.md §3, §4 diagram
 * "NO write path. NO C-STORE outbound.").
 *
 * <p>This project ships the interface but not a wire-level dcm4che
 * implementation. Deployments that need DIMSE plug in an external
 * adapter module that mirrors the seam exactly. {@link InMemoryDimseClient}
 * backs the acceptance scenarios and lets phase-2 wiring be exercised in
 * unit tests without a live PACS.
 */
public interface DimseClient extends AutoCloseable {

    /**
     * Issue a C-FIND request with the supplied DICOM attribute filter
     * (e.g. {@code {"ModalitiesInStudy":"SR"}}). Returns a list of matching
     * studies as DICOM+JSON nodes.
     */
    List<JsonNode> cFind(JsonNode queryAttributes) throws IOException;

    /**
     * Issue a C-MOVE for the matching studies and return the retrieved
     * SR instances as DICOM+JSON nodes (the implementation runs a
     * temporary C-STORE SCP, parses received SR objects, and returns the
     * parsed metadata only — pixel data never leaves the SCP buffer).
     */
    List<JsonNode> cMove(JsonNode queryAttributes) throws IOException;

    /**
     * Issue a C-GET for the matching studies. Same return shape as
     * {@link #cMove(JsonNode)}; chosen by deployments that cannot run an
     * inbound C-STORE listener (firewalled environments).
     */
    List<JsonNode> cGet(JsonNode queryAttributes) throws IOException;

    /** Best-effort indication that the underlying association is alive. */
    default boolean isReady() { return true; }

    @Override
    default void close() { /* default: no-op */ }
}
