package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.List;

//refactor signals map to this object;

/**
 * This interface encapsulate signal storing logic
 * */
public interface ISignalStorage extends Serializable {

    /**
     *
     * Method to read signals for neuron
     * @param neuronId
     * @return list of input signals for neuron
     * */
    List<ISignal> getSignalsForNeuron(Long neuronId);
}
