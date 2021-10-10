package com.rakovpublic.jneuropallium.worker.neuron;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

public interface IResultNeuron<K extends IResultSignal> extends INeuron {
    K getFinalResult();
}
