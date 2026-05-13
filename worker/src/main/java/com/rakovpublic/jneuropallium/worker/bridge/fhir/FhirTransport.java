/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Test seam between {@link FhirClientService} and a real FHIR REST
 * endpoint (06-FHIR.md §3 rule 1, §7).
 *
 * <p><b>Structurally read-only.</b> This interface defines the entire FHIR
 * surface the bridge can use. There is intentionally no
 * {@code create()} / {@code update()} / {@code delete()} / {@code patch()}
 * method, and no method that takes an HTTP verb argument: a code path
 * that issues a non-{@code GET} request against a FHIR resource cannot
 * exist within the bridge because the seam to the wire does not provide
 * one (§3 rule 1, §9 S10).
 *
 * <p>Production wiring constructs a {@code JdkHttpFhirTransport} that uses
 * {@code java.net.http.HttpClient} for {@code GET} only, with bearer /
 * basic / mTLS authentication configured per
 * {@link FhirBridgeConfig.SecurityConfig}. Acceptance scenarios drive an
 * {@link InMemoryFhirTransport} that pumps preloaded JSON resources
 * through the same pipeline.
 *
 * <p>Deployments that prefer the HAPI FHIR client may provide a
 * {@code HapiFhirTransport} that delegates to {@code IGenericClient.read()}
 * / {@code IGenericClient.search()} only — those two methods are the
 * exact surface this interface mirrors. The bridge stays unaware.
 */
public interface FhirTransport extends AutoCloseable {

    /**
     * Read a single resource by absolute or relative reference.
     *
     * <p>{@code reference} may be either {@code "Patient/123"} or
     * {@code "https://server/api/FHIR/R4/Patient/123"}. Returns the parsed
     * JSON resource node, or a JSON {@code null} node if the resource is
     * not found.
     */
    JsonNode read(String reference) throws IOException;

    /**
     * Execute a FHIR search against the configured base URL. The
     * {@code searchExpression} is a relative URL of the form
     * {@code "Observation?category=vital-signs&code=8867-4&patient=abc"}.
     *
     * <p>Returns the parsed {@code Bundle} resource as a
     * {@link com.fasterxml.jackson.databind.JsonNode}. Pagination is the
     * caller's responsibility (the bridge polls each search per
     * {@code pollIntervalSeconds}; the v1 mapper consumes the first page).
     */
    JsonNode search(String searchExpression) throws IOException;

    /**
     * Best-effort indication that the underlying transport is alive and
     * authenticated. Used by §9 S8 (OAuth refresh) to decide whether a
     * poll cycle should attempt to re-authenticate before issuing the
     * search.
     */
    default boolean isReady() { return true; }

    @Override
    default void close() { /* default: no-op */ }
}
