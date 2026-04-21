package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.neurons.harm.HarmGateNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.harm.IHarmGateNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.*;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class HarmInterceptionProcessor implements ISignalProcessor<MotorCommandSignal, IHarmGateNeuron> {

    @Override
    public <I extends ISignal> List<I> process(MotorCommandSignal input, IHarmGateNeuron neuron) {
        List<I> results = new ArrayList<>();
        String planId = input.getActionPlanId() != null ? input.getActionPlanId() : "plan-" + input.getEffectorId();

        // Always emit transparency log for every interception
        TransparencyLogSignal log = new TransparencyLogSignal(planId, "assessment_requested",
            new String[]{neuron.getId() != null ? neuron.getId().toString() : "HarmGate"},
            HarmVerdict.UNCERTAIN, System.currentTimeMillis());
        log.setSourceNeuronId(neuron.getId());
        results.add((I) log);

        // Check assessment cache
        HarmAssessmentSignal cached = neuron.getCachedAssessment(planId);
        if (cached != null) {
            // Apply cached verdict
            results.addAll((List) applyVerdict(cached, input, neuron));
        } else {
            // Cache miss: query consequence model
            neuron.getPendingQueue().add(input);
            ConsequenceQuerySignal query = new ConsequenceQuerySignal(
                planId, new MotorCommandSignal[]{input}, neuron.getSimulationDepth(),
                neuron.getId() != null ? neuron.getId().toString() : "HarmGate");
            query.setSourceNeuronId(neuron.getId());
            results.add((I) query);
        }
        return results;
    }

    private <I extends ISignal> List<I> applyVerdict(HarmAssessmentSignal assessment, MotorCommandSignal input, IHarmGateNeuron neuron) {
        List<I> results = new ArrayList<>();
        TransparencyLogSignal log = new TransparencyLogSignal(assessment.getActionPlanId(),
            "cached_verdict:" + assessment.getVerdict(),
            new String[0], assessment.getVerdict(), System.currentTimeMillis());
        log.setSourceNeuronId(neuron.getId());
        results.add((I) log);

        if (assessment.getVerdict() == HarmVerdict.SAFE) {
            MotorCommandSignal released = input.copySignal();
            released.setExecute(true);
            results.add((I) released);
        }
        return results;
    }

    @Override public String getDescription() { return "HarmInterceptionProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HarmInterceptionProcessor.class; }
    @Override public Class<IHarmGateNeuron> getNeuronClass() { return IHarmGateNeuron.class; }
    @Override public Class<MotorCommandSignal> getSignalClass() { return MotorCommandSignal.class; }
}
