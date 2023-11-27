/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.List;

/**
 * This interface extends standard input and add ability to send signals to study neuron which put the result as input to this neuron net.
 */
public interface INeuronNetInput extends IInitInput {
    /**
     * @param signals studying signals NOTE: the programmer should keep the contract with the input neuron net
     */
    void sendCallBack(List<IInputSignal> signals);
}
