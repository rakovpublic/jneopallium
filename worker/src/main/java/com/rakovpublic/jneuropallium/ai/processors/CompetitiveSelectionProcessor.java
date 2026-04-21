package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.action.ActionSelectionNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.action.IActionSelectionNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompetitiveSelectionProcessor implements ISignalProcessor<MotorCommandSignal, IActionSelectionNeuron> {

    @Override
    public <I extends ISignal> List<I> process(MotorCommandSignal input, IActionSelectionNeuron neuron) {
        List<I> results = new ArrayList<>();
        // Do not accept signals that are already vetoed
        if (input.getActionPlanId() != null && neuron.hasPendingVeto(input.getActionPlanId())) {
            return results;
        }
        neuron.getCandidates().put(
            input.getActionPlanId() != null ? input.getActionPlanId() : String.valueOf(input.getEffectorId()),
            input);
        // Compute softmax-style scores
        Map<String, MotorCommandSignal> candidates = neuron.getCandidates();
        String bestId = null;
        double bestScore = -Double.MAX_VALUE;
        double[] scores = new double[candidates.size()];
        String[] ids = candidates.keySet().toArray(new String[0]);
        for (int i = 0; i < ids.length; i++) {
            MotorCommandSignal cmd = candidates.get(ids[i]);
            scores[i] = cmd.getMagnitudeEstimate() * neuron.getDopamineLevel();
        }
        // Softmax normalization
        double maxScore = 0;
        for (double s : scores) if (s > maxScore) maxScore = s;
        double sumExp = 0;
        for (double s : scores) sumExp += Math.exp(s - maxScore);
        for (int i = 0; i < ids.length; i++) {
            double prob = Math.exp(scores[i] - maxScore) / (sumExp > 0 ? sumExp : 1.0);
            if (prob > bestScore) { bestScore = prob; bestId = ids[i]; }
        }
        double effectiveThreshold = neuron.getConfidenceThreshold() / (neuron.getNorepinephrineLevel() > 0 ? neuron.getNorepinephrineLevel() : 1.0);
        if (bestId != null && bestScore > effectiveThreshold && !neuron.hasPendingVeto(bestId)) {
            MotorCommandSignal winner = candidates.get(bestId).copySignal();
            winner.setExecute(true);
            results.add((I) winner);
        }
        return results;
    }

    @Override public String getDescription() { return "CompetitiveSelectionProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return CompetitiveSelectionProcessor.class; }
    @Override public Class<IActionSelectionNeuron> getNeuronClass() { return IActionSelectionNeuron.class; }
    @Override public Class<MotorCommandSignal> getSignalClass() { return MotorCommandSignal.class; }
}
