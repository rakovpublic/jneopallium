package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.harm.HarmLearningNeuron;
import com.rakovpublic.jneuropallium.ai.signals.slow.HarmFeedbackSignal;
import com.rakovpublic.jneuropallium.ai.signals.slow.HarmModelUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class FeedbackLearningProcessor implements ISignalProcessor<HarmFeedbackSignal, HarmLearningNeuron> {

    @Override
    public <I extends ISignal> List<I> process(HarmFeedbackSignal input, HarmLearningNeuron neuron) {
        List<I> results = new ArrayList<>();
        double lr = neuron.getLearningRate();
        double bias = neuron.getConservatismBias(); // always > 1.0
        double[] observed = input.getObservedHumanStateChange();
        if (observed == null) return results;

        double[] predicted = neuron.getPredictedHarmForPlan(input.getActionPlanId());
        if (predicted == null) predicted = new double[observed.length];

        double[] weightDeltas = new double[observed.length];
        for (int i = 0; i < Math.min(observed.length, predicted.length); i++) {
            double delta = observed[i] - predicted[i];
            if (delta > 0) {
                // Harm underestimated: learn more cautiously (faster update)
                weightDeltas[i] = lr * bias * delta;
            } else {
                // Harm overestimated: relax caution (slower update)
                weightDeltas[i] = lr * (1.0 / bias) * Math.abs(delta);
            }
        }

        HarmModelUpdateSignal update = new HarmModelUpdateSignal(
            "ConsequenceModelNeuron", weightDeltas, "FeedbackLearning:" + input.getActionPlanId());
        update.setSourceNeuronId(neuron.getId());
        results.add((I) update);
        return results;
    }

    @Override public String getDescription() { return "FeedbackLearningProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return FeedbackLearningProcessor.class; }
    @Override public Class<HarmLearningNeuron> getNeuronClass() { return HarmLearningNeuron.class; }
    @Override public Class<HarmFeedbackSignal> getSignalClass() { return HarmFeedbackSignal.class; }
}
