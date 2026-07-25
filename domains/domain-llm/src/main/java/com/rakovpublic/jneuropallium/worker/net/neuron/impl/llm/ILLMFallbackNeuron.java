package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface ILLMFallbackNeuron extends INeuron {
    void recordSuccess();
    void recordFailure();
    LLMFallbackNeuron.CircuitState getState();
    boolean isLLMCallAllowed();
    int getFailureThreshold();
    void setFailureThreshold(int failureThreshold);
    long getHalfOpenProbeIntervalMs();
    void setHalfOpenProbeIntervalMs(long halfOpenProbeIntervalMs);
    int getConsecutiveFailures();
    void reset();
}
