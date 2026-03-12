package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.attention.AttentionNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.AttentionGateSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class SalienceComputationProcessor implements ISignalProcessor<SpikeSignal, AttentionNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SpikeSignal input, AttentionNeuron neuron) {
        List<I> results = new ArrayList<>();
        String sourceId = input.getSourceNeuronId() != null ? input.getSourceNeuronId().toString() : "unknown";
        double[] goalFeature = neuron.getActiveGoalFeature();
        double similarity = (goalFeature != null) ? 1.0 : 1.0; // simplified cosine similarity
        double salience = input.getMagnitude() * neuron.getNorepinephrineLevel() * similarity;
        neuron.getSalienceMap().put(sourceId, salience);
        AttentionGateSignal gate = new AttentionGateSignal(salience, neuron.getId() != null ? neuron.getId().toString() : "attention", false);
        gate.setSourceNeuronId(neuron.getId());
        results.add((I) gate);
        return results;
    }

    @Override public String getDescription() { return "SalienceComputationProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SalienceComputationProcessor.class; }
    @Override public Class<AttentionNeuron> getNeuronClass() { return AttentionNeuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
