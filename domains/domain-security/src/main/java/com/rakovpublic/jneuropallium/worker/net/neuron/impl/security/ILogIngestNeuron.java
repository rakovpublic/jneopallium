package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;

import java.util.Map;

public interface ILogIngestNeuron extends IModulatableNeuron {
    LogEventSignal ingest(String source, LogLevel level, Map<String, String> fields, long timestamp);
    long getAccepted();
}
