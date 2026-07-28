/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Test seam between {@link XapiClientService} and a real xAPI Learning
 * Record Store (14-LTI-XAPI.md §4 architecture diagram, §7).
 *
 * <p>The seam exposes two operations:
 *
 * <ul>
 *   <li>{@link #pollStatements} — pull statements from the LRS as a
 *       client. Used in PULL mode.</li>
 *   <li>{@link #postStatement} — POST an xAPI statement back to the LRS
 *       <b>only for the bridge's own advisory verbs</b>
 *       ({@code recommended}, {@code experienced} for hints, {@code
 *       inferred} for affect). The implementation does <b>not</b> permit
 *       arbitrary write of statements on behalf of the learner — the
 *       {@link LtiAdvisoryOutputAggregator} validates the actor identity
 *       and verb before posting.</li>
 * </ul>
 *
 * <p>Production wiring constructs a {@code JdkHttpXapiTransport} backed
 * by {@code java.net.http.HttpClient}. Acceptance scenarios drive an
 * {@link InMemoryXapiTransport} that holds a preloaded queue.
 */
public interface XapiTransport extends AutoCloseable {

    /**
     * Pull a page of statements from the LRS. The {@code query} is a
     * relative URL of the form
     * {@code "statements?verb=...&since=2026-01-01T00:00:00Z"}. Returns
     * the parsed response — either a JSON array or an object with a
     * {@code statements} field.
     */
    JsonNode pollStatements(String query) throws IOException;

    /**
     * POST a single xAPI statement to the LRS. Returns the statement id
     * assigned by the LRS, or {@code null} when the transport has no
     * persistent backing (in-memory tests).
     */
    String postStatement(JsonNode statement) throws IOException;

    /**
     * POST an AGS {@code Score} to the LMS. The {@code lineItemUrl} comes
     * from the LTI launch claims (line-item URL) — the bridge does not
     * know what line items exist until a learner has been launched against
     * them.
     */
    default void postAgsScore(String lineItemUrl, JsonNode score) throws IOException {
        throw new UnsupportedOperationException(
                "AGS scoring not supported by this transport");
    }

    /** Best-effort liveness check. */
    default boolean isReady() { return true; }

    @Override
    default void close() { /* default: no-op */ }
}
