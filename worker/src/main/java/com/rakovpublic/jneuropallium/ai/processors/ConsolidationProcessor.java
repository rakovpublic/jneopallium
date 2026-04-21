package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.memory.ILongTermMemoryNeuron;
import com.rakovpublic.jneuropallium.ai.signals.slow.ConsolidationSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class ConsolidationProcessor implements ISignalProcessor<ConsolidationSignal, ILongTermMemoryNeuron> {

    @Override
    public <I extends ISignal> List<I> process(ConsolidationSignal input, ILongTermMemoryNeuron neuron) {
        if (input.isPromote() && input.getImportance() > neuron.getImportanceThreshold()) {
            // Hebbian write: add a new pattern
            double[] pattern = new double[]{input.getImportance()};
            double[] value = new double[]{1.0};
            neuron.getPatterns().add(pattern);
            neuron.getValues().add(value);
            // Strengthen existing weights
            for (int i = 0; i < neuron.getValues().size(); i++) {
                double[] v = neuron.getValues().get(i);
                for (int j = 0; j < v.length; j++) {
                    v[j] += neuron.getLearningRate() * input.getImportance() * v[j];
                }
            }
        } else {
            // Forgetting
            double rate = neuron.getForgettingRate();
            for (double[] v : neuron.getValues()) {
                for (int i = 0; i < v.length; i++) v[i] *= (1.0 - rate);
            }
        }
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "ConsolidationProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ConsolidationProcessor.class; }
    @Override public Class<ILongTermMemoryNeuron> getNeuronClass() { return ILongTermMemoryNeuron.class; }
    @Override public Class<ConsolidationSignal> getSignalClass() { return ConsolidationSignal.class; }
}
