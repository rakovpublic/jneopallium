package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

import com.rakovpublic.jneuropallium.ai.neurons.base.IModulatableNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;

public interface IAffectObserverNeuron extends IModulatableNeuron {
    AffectObservationSignal infer(double engagement, double accuracy);
    double getEngagementEwma();
    double getAccuracyEwma();
}
