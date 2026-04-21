package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
