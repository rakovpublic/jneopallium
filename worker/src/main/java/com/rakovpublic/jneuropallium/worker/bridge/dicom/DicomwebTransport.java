/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Test seam between {@link DicomwebClient} and a real DICOMweb endpoint
 * (07-DICOM.md §3, §4, §7).
 *
 * <p><b>Structurally read-only.</b> This interface is the entire DICOMweb
 * surface the bridge can use. There is intentionally no {@code post()},
 * {@code put()}, {@code stowRs()}, {@code delete()} or any method that
 * accepts an HTTP verb argument or a request body: a code path that
 * pushes a DICOM instance back to the PACS cannot exist within the
 * bridge because the seam to the wire does not provide one
 * (07-DICOM.md §3, §4 diagram "NO C-STORE outbound. NO STOW-RS.").
 *
 * <p>Production wiring constructs a {@code JdkHttpDicomwebTransport}; an
 * {@link InMemoryDicomwebTransport} backs the acceptance scenarios.
 */
public interface DicomwebTransport extends AutoCloseable {

    /**
     * QIDO-RS search. {@code path} is a relative URL with leading slash
     * (e.g. {@code "/studies?ModalitiesInStudy=SR"}). Returns the parsed
     * JSON array of matching study/series/instance attributes, or an
     * empty array if there are no matches.
     */
    JsonNode qido(String path) throws IOException;

    /**
     * WADO-RS metadata fetch (no pixel data — §10 R1 mandates
     * {@code accept=application/dicom+json}). {@code path} is a relative
     * URL identifying an instance metadata endpoint. Returns the JSON
     * array of instances, or an empty array if not found.
     */
    JsonNode wadoMetadata(String path) throws IOException;

    /** Best-effort indication that the underlying transport is alive and authenticated. */
    default boolean isReady() { return true; }

    @Override
    default void close() { /* default: no-op */ }
}
