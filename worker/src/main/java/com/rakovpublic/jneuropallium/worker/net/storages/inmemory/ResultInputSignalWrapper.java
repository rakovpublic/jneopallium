/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.AbstractSignal;

public class ResultInputSignalWrapper<T> extends AbstractSignal<T> implements IInputSignal<T>  {
    private IResultSignal<T> inputSignal;
    public ResultInputSignalWrapper(T value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }
    public ResultInputSignalWrapper(IResultSignal<T> inputSignal) {
        super(inputSignal.getValue(), inputSignal.getSourceLayerId(), inputSignal.getSourceNeuronId(), inputSignal.getTimeAlive(), inputSignal.getDescription(), inputSignal.isFromExternalNet(), inputSignal.getInputName(), inputSignal.isNeedToRemoveDuringLearning(), inputSignal.isNeedToProcessDuringLearning(), inputSignal.getName());
        this.inputSignal = inputSignal;
    }


    @Override
    public Class<? extends ISignal<T>> getCurrentSignalClass() {
        return inputSignal.getCurrentSignalClass();
    }

    @Override
    public Class<T> getParamClass() {
        return inputSignal.getParamClass();
    }

    @Override
    public ISignal copySignal() {
        return new ResultInputSignalWrapper(inputSignal);
    }
}
