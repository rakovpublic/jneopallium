package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

import java.util.Optional;

public interface ILLMKnowledgeNeuron extends INeuron, ILLMCapable {
    void submitQuery(LLMQuerySignal query);
    Optional<LLMResponseSignal> getCachedResponse(String queryId);
    boolean isLLMAvailable();
    void setLLMEndpoint(String endpoint);
    void setMaxLatency(long milliseconds);
    LLMConfig getConfig();
    void setConfig(LLMConfig config);
}
