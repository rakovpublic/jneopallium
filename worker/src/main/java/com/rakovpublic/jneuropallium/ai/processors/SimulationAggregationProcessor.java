package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.model.HarmThreshold;
import com.rakovpublic.jneuropallium.ai.neurons.harm.HarmEvaluationNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.harm.IHarmEvaluationNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ConsequenceSimulationSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SimulationAggregationProcessor implements ISignalProcessor<ConsequenceSimulationSignal, IHarmEvaluationNeuron> {

    // Dimension names for threshold lookup
    private static final String[] DIM_NAMES = {"physicalIntegrity", "autonomy", "resource", "information", "emotional"};

    @Override
    public <I extends ISignal> List<I> process(ConsequenceSimulationSignal input, IHarmEvaluationNeuron neuron) {
        List<I> results = new ArrayList<>();
        String planId = input.getActionPlanId();
        neuron.getPendingSimulations().computeIfAbsent(planId, k -> new ArrayList<>()).add(input);

        List<ConsequenceSimulationSignal> allSteps = neuron.getPendingSimulations().get(planId);
        HarmVerdict worstVerdict = HarmVerdict.SAFE;
        List<String> triggeringConditions = new ArrayList<>();
        double[] worstHarmScores = new double[5];

        for (ConsequenceSimulationSignal step : allSteps) {
            double[] impact = step.getHumanStateImpact();
            if (impact == null) continue;
            for (int dim = 0; dim < Math.min(impact.length, 5); dim++) {
                double val = impact[dim];
                worstHarmScores[dim] = Math.max(worstHarmScores[dim], Math.abs(val));
                HarmThreshold threshold = neuron.getDimensionThresholds().get(DIM_NAMES[dim]);
                if (threshold == null) continue;
                // CRITICAL: physicalIntegrity at catastrophic threshold = CATASTROPHIC (unoverridable)
                if (dim == 0 && val <= -threshold.getCatastrophicThreshold()) {
                    worstVerdict = HarmVerdict.CATASTROPHIC;
                    triggeringConditions.add("step=" + step.getSimulationStep() + " dim=physicalIntegrity val=" + val);
                } else if (worstVerdict != HarmVerdict.CATASTROPHIC && val <= -threshold.getHarmThreshold()) {
                    worstVerdict = HarmVerdict.HARMFUL;
                    triggeringConditions.add("step=" + step.getSimulationStep() + " dim=" + DIM_NAMES[dim] + " val=" + val);
                } else if (worstVerdict == HarmVerdict.SAFE && Math.abs(val) > threshold.getUncertainThreshold()) {
                    worstVerdict = HarmVerdict.UNCERTAIN;
                    triggeringConditions.add("step=" + step.getSimulationStep() + " dim=" + DIM_NAMES[dim] + " uncertain");
                }
            }
        }

        HarmAssessmentSignal assessment = new HarmAssessmentSignal(planId, worstVerdict, worstHarmScores,
            triggeringConditions.toArray(new String[0]), 0.8);
        assessment.setSourceNeuronId(neuron.getId());
        results.add((I) assessment);
        return results;
    }

    @Override public String getDescription() { return "SimulationAggregationProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return SimulationAggregationProcessor.class; }
    @Override public Class<IHarmEvaluationNeuron> getNeuronClass() { return IHarmEvaluationNeuron.class; }
    @Override public Class<ConsequenceSimulationSignal> getSignalClass() { return ConsequenceSimulationSignal.class; }
}
