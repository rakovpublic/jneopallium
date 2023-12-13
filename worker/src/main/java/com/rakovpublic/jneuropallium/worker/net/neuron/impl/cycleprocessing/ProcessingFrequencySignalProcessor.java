package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class ProcessingFrequencySignalProcessor implements ISignalProcessor<ProcessingFrequencySignal, CycleNeuron> {
    private final static String DESCRIPTION = "Processor which changes processing frequency for specific signal class";

    @Override
    public <I extends ISignal> List<I> process(ProcessingFrequencySignal input, CycleNeuron neuron) {
        neuron.getSignalProcessingFrequencyMap().put(input.getValue().getSignalClass(), input.getValue().getFrequency());
        return new ArrayList<>();
    }

    @Override
    public String getDescription() {
        return DESCRIPTION;
    }

    @Override
    public Boolean hasMerger() {
        return false;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return ProcessingFrequencySignalProcessor.class;
    }

    @Override
    public Class<CycleNeuron> getNeuronClass() {
        return CycleNeuron.class;
    }
}
