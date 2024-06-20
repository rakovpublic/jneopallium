/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class DoubleSignal extends AbstractSignal<Double> implements IInputSignal<Double> {
    public Class<? extends ISignal<Double>> currentSignalClass = DoubleSignal.class;
    public Class<Double> paramClass = Double.class;
    public DoubleSignal() {
    }

    public DoubleSignal(Double value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name,DoubleSignal.class.getCanonicalName());
    }

    @Override
    public Class<? extends ISignal<Double>> getCurrentSignalClass() {
        return currentSignalClass;
    }

    @Override
    public Class<Double> getParamClass() {
        return paramClass;
    }

    @Override
    public DoubleSignal copySignal() {
        return new DoubleSignal(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }
}
