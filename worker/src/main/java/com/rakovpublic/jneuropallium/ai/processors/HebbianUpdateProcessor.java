package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.learning.HebbianLearningNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.learning.IHebbianLearningNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ErrorSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class HebbianUpdateProcessor implements ISignalProcessor<ErrorSignal, IHebbianLearningNeuron> {

    @Override
    public <I extends ISignal> List<I> process(ErrorSignal input, IHebbianLearningNeuron neuron) {
        // Only apply weight update when ACh gate is open
        if (neuron.getAchLevel() > neuron.getAchThreshold()) {
            double deltaWeight = neuron.getLearningRate() * input.getDelta();
            neuron.applyWeightDelta(input.getTargetNeuronId(), deltaWeight);
        }
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "HebbianUpdateProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HebbianUpdateProcessor.class; }
    @Override public Class<IHebbianLearningNeuron> getNeuronClass() { return IHebbianLearningNeuron.class; }
    @Override public Class<ErrorSignal> getSignalClass() { return ErrorSignal.class; }
}
