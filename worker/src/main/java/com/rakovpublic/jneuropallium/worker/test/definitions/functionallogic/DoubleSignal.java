/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class DoubleSignal extends AbstractSignal<Double> {

    public DoubleSignal(Double value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }

    @Override
    public Class<? extends ISignal<Double>> getCurrentSignalClass() {
        return null;
    }

    @Override
    public Class<Double> getParamClass() {
        return null;
    }

    @Override
    public <K extends ISignal<Double>> K copySignal() {
        return null;
    }
}
