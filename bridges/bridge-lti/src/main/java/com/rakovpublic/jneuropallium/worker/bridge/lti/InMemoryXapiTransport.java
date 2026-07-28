/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * In-memory {@link XapiTransport} for tests and the acceptance scenarios
 * (14-LTI-XAPI.md §9 S7, S9, S11, S12). Holds preloaded statement lists
 * by query, and records POSTed statements + AGS scores so the test can
 * assert on egress.
 */
public final class InMemoryXapiTransport implements XapiTransport {

    private final ObjectMapper mapper;
    private final Map<String, List<JsonNode>> statementsByQuery = new LinkedHashMap<>();
    private final List<JsonNode> postedStatements = new ArrayList<>();
    private final List<PostedScore> postedScores = new ArrayList<>();
    private boolean ready = true;

    public InMemoryXapiTransport() {
        this(new ObjectMapper());
    }

    public InMemoryXapiTransport(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** Seed a list of statements for one pollStatements query. */
    public InMemoryXapiTransport putStatements(String query, JsonNode... statements) {
        List<JsonNode> list = new ArrayList<>();
        if (statements != null) for (JsonNode s : statements) if (s != null) list.add(s);
        statementsByQuery.put(query, list);
        return this;
    }

    public InMemoryXapiTransport setReady(boolean ready) {
        this.ready = ready;
        return this;
    }

    public List<JsonNode> postedStatements() { return List.copyOf(postedStatements); }
    public List<PostedScore> postedScores() { return List.copyOf(postedScores); }

    @Override
    public JsonNode pollStatements(String query) throws IOException {
        List<JsonNode> list = statementsByQuery.get(query);
        ObjectNode body = mapper.createObjectNode();
        ArrayNode arr = body.putArray("statements");
        if (list != null) for (JsonNode n : list) arr.add(n);
        body.put("more", "");
        // Drain after first poll so a subsequent call returns empty (mirrors
        // an LRS that respects {@code since=}).
        if (list != null) list.clear();
        return body;
    }

    @Override
    public String postStatement(JsonNode statement) throws IOException {
        postedStatements.add(statement.deepCopy());
        return UUID.randomUUID().toString();
    }

    @Override
    public void postAgsScore(String lineItemUrl, JsonNode score) throws IOException {
        postedScores.add(new PostedScore(lineItemUrl, score.deepCopy()));
    }

    @Override
    public boolean isReady() { return ready; }

    /** Recorded AGS post for assertions. */
    public record PostedScore(String lineItemUrl, JsonNode body) {}
}
