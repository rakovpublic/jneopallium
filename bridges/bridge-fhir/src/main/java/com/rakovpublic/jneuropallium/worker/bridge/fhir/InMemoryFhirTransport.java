/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * In-memory {@link FhirTransport} for tests and the acceptance scenarios
 * (06-FHIR.md §9 S7, S9, S11). Holds parsed JSON resources by
 * {@code "Type/id"} reference and answers searches from a per-search
 * preloaded {@code Bundle}.
 *
 * <p>This transport has no write surface — it mirrors the interface
 * exactly, which is the entire structural defence the bridge relies on
 * (§3 rule 1).
 */
public final class InMemoryFhirTransport implements FhirTransport {

    private final ObjectMapper mapper;
    private final Map<String, JsonNode> resourcesByReference = new LinkedHashMap<>();
    private final Map<String, JsonNode> bundlesBySearch = new LinkedHashMap<>();

    private boolean ready = true;

    public InMemoryFhirTransport() {
        this(new ObjectMapper());
    }

    public InMemoryFhirTransport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Register a resource under its canonical {@code Type/id} reference. */
    public InMemoryFhirTransport putResource(String reference, JsonNode resource) {
        resourcesByReference.put(reference, resource);
        return this;
    }

    /** Convenience — register a resource from raw JSON. */
    public InMemoryFhirTransport putResource(String reference, String resourceJson) throws IOException {
        return putResource(reference, mapper.readTree(resourceJson));
    }

    /**
     * Register a {@code Bundle} response for a given search expression.
     * Searches are matched literally; the configured {@code {pid}}
     * substitution is the caller's responsibility before registration.
     */
    public InMemoryFhirTransport putSearch(String searchExpression, JsonNode bundle) {
        bundlesBySearch.put(searchExpression, bundle);
        return this;
    }

    /** Convenience overload — wrap an array of resources into a synthetic Bundle. */
    public InMemoryFhirTransport putSearchEntries(String searchExpression, JsonNode... entries) {
        ObjectNode bundle = mapper.createObjectNode();
        bundle.put("resourceType", "Bundle");
        bundle.put("type", "searchset");
        ArrayNode arr = bundle.putArray("entry");
        if (entries != null) {
            for (JsonNode e : entries) {
                if (e == null) continue;
                ObjectNode entry = arr.addObject();
                entry.set("resource", e);
            }
        }
        bundle.put("total", arr.size());
        bundlesBySearch.put(searchExpression, bundle);
        return this;
    }

    /** Force the transport to report unready (e.g. simulating an expired token; §9 S8). */
    public InMemoryFhirTransport setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    @Override
    public JsonNode read(String reference) {
        JsonNode r = resourcesByReference.get(reference);
        return r == null ? NullNode.getInstance() : r;
    }

    @Override
    public JsonNode search(String searchExpression) {
        JsonNode b = bundlesBySearch.get(searchExpression);
        if (b != null) return b;
        ObjectNode empty = mapper.createObjectNode();
        empty.put("resourceType", "Bundle");
        empty.put("type", "searchset");
        empty.putArray("entry");
        empty.put("total", 0);
        return empty;
    }

    @Override
    public boolean isReady() { return ready; }
}
