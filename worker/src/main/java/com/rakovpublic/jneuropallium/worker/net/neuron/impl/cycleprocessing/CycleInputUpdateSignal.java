/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class CycleInputUpdateSignal extends AbstractSignal<CycleInputSignalUpdateItem> implements ISignal<CycleInputSignalUpdateItem> {
    public CycleInputUpdateSignal(CycleInputSignalUpdateItem value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name, CycleInputUpdateSignal.class.getCanonicalName());
    }

    @Override
    public Class<? extends ISignal<CycleInputSignalUpdateItem>> getCurrentSignalClass() {
        return CycleInputUpdateSignal.class;
    }

    @Override
    public Class<CycleInputSignalUpdateItem> getParamClass() {
        return CycleInputSignalUpdateItem.class;
    }

    @Override
    public CycleInputUpdateSignal copySignal() {
        return new CycleInputUpdateSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(), this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName(), this.isNeedToRemoveDuringLearning(), this.isNeedToProcessDuringLearning(), ProcessingFrequency.class.getName());
    }
}
