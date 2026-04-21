package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.attention.AttentionNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.attention.IAttentionNeuron;
import com.rakovpublic.jneuropallium.ai.signals.slow.GoalUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class SalienceGoalProcessor implements ISignalProcessor<GoalUpdateSignal, IAttentionNeuron> {

    @Override
    public <I extends ISignal> List<I> process(GoalUpdateSignal input, IAttentionNeuron neuron) {
        neuron.getGoalFeatureMap().put(input.getGoalId(), new double[]{input.getPriority()});
        // Update active goal feature to highest priority
        double maxPriority = -1;
        double[] bestFeature = null;
        for (double[] feature : neuron.getGoalFeatureMap().values()) {
            if (feature.length > 0 && feature[0] > maxPriority) {
                maxPriority = feature[0];
                bestFeature = feature;
            }
        }
        neuron.setActiveGoalFeature(bestFeature);
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "SalienceGoalProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SalienceGoalProcessor.class; }
    @Override public Class<IAttentionNeuron> getNeuronClass() { return IAttentionNeuron.class; }
    @Override public Class<GoalUpdateSignal> getSignalClass() { return GoalUpdateSignal.class; }
}
