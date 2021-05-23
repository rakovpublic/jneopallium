package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class MultiplyCycleSignal extends AbstractSignal<Float> implements ISignal<Float> {
    public MultiplyCycleSignal(Float value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description);
    }

    @Override
    public Class<? extends ISignal<Float>> getCurrentSignalClass() {
        return MultiplyCycleSignal.class;
    }

    @Override
    public Class<Float> getParamClass() {
        return Float.class;
    }
}
