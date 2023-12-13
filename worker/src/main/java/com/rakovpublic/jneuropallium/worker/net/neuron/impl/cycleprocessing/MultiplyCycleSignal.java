package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class MultiplyCycleSignal extends AbstractSignal<Float> implements ISignal<Float> {
    public MultiplyCycleSignal(Float value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, true, true, MultiplyCycleSignal.class.getName());
    }

    @Override
    public Class<? extends ISignal<Float>> getCurrentSignalClass() {
        return MultiplyCycleSignal.class;
    }

    @Override
    public Class<Float> getParamClass() {
        return Float.class;
    }

    @Override
    public MultiplyCycleSignal copySignal() {
        return new MultiplyCycleSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(), this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName());
    }
}
