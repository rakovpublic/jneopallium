package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalProcessor;

import java.util.ArrayList;
import java.util.List;

public class MultiplyCycleSignalProcessor implements ISignalProcessor<MultiplyCycleSignal, CycleNeuron> {
    @Override
    public <I extends ISignal> List<I> process(MultiplyCycleSignal input, CycleNeuron neuron) {
        neuron.setLoopCount((int) (neuron.getLoopCount() * input.getValue()));
        if (neuron.getInputStatusMeta() != null) {
            neuron.getInputStatusMeta().setUpdateOnceInNRuns(neuron.getLoopCount());
        }
        return new ArrayList<I>();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return MultiplyCycleSignalProcessor.class;
    }

    @Override
    public Class<CycleNeuron> getNeuronClass() {
        return CycleNeuron.class;
    }
}
