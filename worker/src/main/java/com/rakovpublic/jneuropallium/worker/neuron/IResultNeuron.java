package com.rakovpublic.jneuropallium.worker.neuron;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;


/**
 * The neuron associated with appropriate result
 */
public interface IResultNeuron<K extends IResultSignal> extends INeuron {
    /**
     * @return the result object
     */
    K getFinalResult();
}
