package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

/**
 * This interface extends standard input and add ability to send signals to study neuron which put the result as input to this neuron net.
 */
public interface INeuronNetInput extends IInitInput {
    /**
     * @param signals studying signals NOTE: the programmer should keep the contract with the input neuron net
     */
    void sendCallBack(List<ISignal> signals);
}
