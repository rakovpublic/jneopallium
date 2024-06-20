package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class InputIntSignal extends IntSignal implements IInputSignal<Integer> {
    public InputIntSignal() {

    }

    public InputIntSignal(Integer value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }

    @Override
    public Class<? extends ISignal<Integer>> getCurrentSignalClass() {
        return IntSignal.class;
    }
}
