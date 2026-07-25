/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

/**
 * Payload for LLMQuerySignal.
 * Carries the query text, context snapshot, priority, and a unique query identifier.
 * Slow loop only (epoch 2) — never block fast-loop sensorimotor processing.
 */
public class LLMQueryItem {

    private String queryId;
    private String queryText;
    private String context;
    /** Higher value = higher priority. */
    private int priority;

    public LLMQueryItem() {
    }

    public LLMQueryItem(String queryId, String queryText, String context, int priority) {
        this.queryId = queryId;
        this.queryText = queryText;
        this.context = context;
        this.priority = priority;
    }

    public String getQueryId() {
        return queryId;
    }

    public void setQueryId(String queryId) {
        this.queryId = queryId;
    }

    public String getQueryText() {
        return queryText;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }
}
