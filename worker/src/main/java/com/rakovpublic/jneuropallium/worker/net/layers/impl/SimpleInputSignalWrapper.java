/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.AbstractSignal;

public class SimpleInputSignalWrapper<T> extends AbstractSignal<T> implements IInputSignal<T> {
    private ISignal<T> signal;

    public SimpleInputSignalWrapper(ISignal<T> inputSignal) {

        super(inputSignal.getValue(), inputSignal.getSourceLayerId(), inputSignal.getSourceNeuronId(), inputSignal.getTimeAlive(), inputSignal.getDescription(), inputSignal.isFromExternalNet(), inputSignal.getInputName(), inputSignal.isNeedToRemoveDuringLearning(), inputSignal.isNeedToProcessDuringLearning(), inputSignal.getName());
        this.signal = inputSignal.copySignal();

    }

    public SimpleInputSignalWrapper(T value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }

    @Override
    public Class<? extends ISignal> getCurrentSignalClass() {
        return signal.getCurrentSignalClass();
    }

    @Override
    public Class getParamClass() {
        return signal.getParamClass();
    }

    @Override
    public ISignal copySignal() {
        return new SimpleInputSignalWrapper(this.signal);
    }
}
