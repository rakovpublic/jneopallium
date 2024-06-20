/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class IntSignal extends AbstractSignal<Integer> implements IInputSignal<Integer> {
    public  Class<? extends ISignal<Integer>> currentSignalClass = IntSignal.class;
    public Class<Integer> paramClass = Integer.class;
    public IntSignal() {
    }

    public IntSignal(Integer value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name,IntSignal.class.getCanonicalName());
    }

    @Override
    public Class<? extends ISignal<Integer>> getCurrentSignalClass() {
        return currentSignalClass;
    }

    @Override
    public Class<Integer> getParamClass() {
        return Integer.class;
    }

    @Override
    public IntSignal copySignal() {
        return new IntSignal(value, sourceLayer,  sourceNeuron,  timeAlive,  description,  fromExternalNet,  inputName,  needToRemoveDuringLearning,  needToProcessDuringLearning,  name);
    }
}
