/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.Optional;

/**
 * Extension of INeuron for neurons that can issue advisory queries to an LLM endpoint.
 *
 * Design constraints (from CLAUDE.md):
 * <ul>
 *   <li>LLM queries run on the slow loop only — never block fast-loop processing.</li>
 *   <li>LLM responses are untrusted until cross-validated.</li>
 *   <li>LLM integration is disabled by default.</li>
 * </ul>
 */
public interface ILLMCapable extends INeuron {

    /**
     * Submit a query to the LLM asynchronously.
     * Implementations must use CompletableFuture to avoid blocking.
     *
     * @param query the query signal to dispatch
     */
    void submitQuery(LLMQuerySignal query);

    /**
     * Retrieve a cached response by query id, if one exists.
     *
     * @param queryId the id of the original query
     * @return cached response or empty
     */
    Optional<LLMResponseSignal> getCachedResponse(String queryId);

    /**
     * @return true if the LLM endpoint is reachable and enabled
     */
    boolean isLLMAvailable();

    /**
     * Configure the LLM endpoint URL (Ollama, OpenAI-compatible, etc.).
     *
     * @param endpoint base URL of the LLM service
     */
    void setLLMEndpoint(String endpoint);

    /**
     * Set the maximum acceptable latency for LLM responses.
     * Queries exceeding this threshold produce a LLMTimeoutSignal.
     *
     * @param milliseconds latency threshold in ms
     */
    void setMaxLatency(long milliseconds);
}
