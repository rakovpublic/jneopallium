package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;

public interface IResponseObserverNeuron extends IModulatableNeuron {
    double observe(ResponseSignal s);
    int getTotalResponses();
    int getTotalCorrect();
    double getAccuracy();
    double getLastError();
    long getLastLatencyMs();
}
