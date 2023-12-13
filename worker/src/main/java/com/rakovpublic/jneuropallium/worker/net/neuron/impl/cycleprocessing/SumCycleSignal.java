package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class SumCycleSignal extends AbstractSignal<Integer> implements ISignal<Integer> {

    public SumCycleSignal(Integer value, java.lang.Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, true, true, SumCycleSignal.class.getName());
    }

    @Override
    public Class<? extends ISignal<Integer>> getCurrentSignalClass() {
        return SumCycleSignal.class;
    }

    @Override
    public Class<Integer> getParamClass() {
        return Integer.class;
    }

    @Override
    public SumCycleSignal copySignal() {
        return new SumCycleSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(), this.getTimeAlive(), getInputName(), isFromExternalNet(), getDescription());
    }
}
