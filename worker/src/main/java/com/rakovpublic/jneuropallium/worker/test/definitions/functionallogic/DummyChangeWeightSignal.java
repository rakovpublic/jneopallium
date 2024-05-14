/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class DummyChangeWeightSignal extends AbstractSignal<Integer> {
    public DummyChangeWeightSignal(Integer value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }

    @Override
    public Class<? extends ISignal<Integer>> getCurrentSignalClass() {
        return IntSignal.class;
    }

    @Override
    public Class<Integer> getParamClass() {
        return Integer.class;
    }

    @Override
    public DummyChangeWeightSignal copySignal() {
        return new DummyChangeWeightSignal(value, sourceLayer,  sourceNeuron,  timeAlive,  description,  fromExternalNet,  inputName,  needToRemoveDuringLearning,  needToProcessDuringLearning,  name);
    }
}
