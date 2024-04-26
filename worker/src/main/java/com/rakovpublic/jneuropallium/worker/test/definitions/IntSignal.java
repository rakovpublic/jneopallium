/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class IntSignal extends AbstractSignal<Integer> {
    public IntSignal(Integer value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }

    @Override
    public Class<? extends ISignal<Integer>> getCurrentSignalClass() {
        return null;
    }

    @Override
    public Class<Integer> getParamClass() {
        return null;
    }

    @Override
    public <K extends ISignal<Integer>> K copySignal() {
        return null;
    }
}
