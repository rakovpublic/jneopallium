package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public interface ILLMKnowledgeNeuron extends INeuron, ILLMCapable {
    void submitQuery(LLMQuerySignal query);
    Optional<LLMResponseSignal> getCachedResponse(String queryId);
    boolean isLLMAvailable();
    void setLLMEndpoint(String endpoint);
    void setMaxLatency(long milliseconds);
    LLMConfig getConfig();
    void setConfig(LLMConfig config);
}
