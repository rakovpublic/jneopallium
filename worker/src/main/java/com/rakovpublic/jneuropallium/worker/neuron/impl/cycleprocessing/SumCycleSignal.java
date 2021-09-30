package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class SumCycleSignal extends AbstractSignal<Integer> implements ISignal<Integer> {

    public SumCycleSignal(Integer value, java.lang.Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description,fromExternalNet);
    }

    @Override
    public Class<? extends ISignal<Integer>> getCurrentSignalClass() {
        return SumCycleSignal.class;
    }

    @Override
    public Class<Integer> getParamClass() {
        return Integer.class;
    }
}
