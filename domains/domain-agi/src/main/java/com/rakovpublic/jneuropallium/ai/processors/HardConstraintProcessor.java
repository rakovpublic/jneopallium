package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.model.AbsoluteConstraint;
import com.rakovpublic.jneuropallium.ai.neurons.harm.IEthicalPriorityNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class HardConstraintProcessor implements ISignalProcessor<HarmAssessmentSignal, IEthicalPriorityNeuron> {

    @Override
    public <I extends ISignal> List<I> process(HarmAssessmentSignal input, IEthicalPriorityNeuron neuron) {
        List<I> results = new ArrayList<>();
        HarmVerdict verdict = input.getVerdict();

        // Evaluate all hard constraints — can only escalate, never downgrade
        for (AbsoluteConstraint constraint : neuron.getHardConstraints()) {
            if (constraint.getTrigger().test(input)) {
                HarmVerdict escalatedVerdict = constraint.getEscalateTo();
                if (ordinal(escalatedVerdict) > ordinal(verdict)) {
                    verdict = escalatedVerdict;
                }
            }
        }

        if (verdict != input.getVerdict()) {
            HarmAssessmentSignal escalated = new HarmAssessmentSignal(
                input.getActionPlanId(), verdict, input.getHarmScores(),
                input.getTriggeringConditions(), input.getConfidence());
            escalated.setSourceNeuronId(neuron.getId());
            results.add((I) escalated);
        } else {
            results.add((I) input);
        }
        return results;
    }

    private int ordinal(HarmVerdict v) {
        switch (v) { case SAFE: return 0; case UNCERTAIN: return 1; case HARMFUL: return 2; case CATASTROPHIC: return 3; }
        return 0;
    }

    @Override public String getDescription() { return "HardConstraintProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HardConstraintProcessor.class; }
    @Override public Class<IEthicalPriorityNeuron> getNeuronClass() { return IEthicalPriorityNeuron.class; }
    @Override public Class<HarmAssessmentSignal> getSignalClass() { return HarmAssessmentSignal.class; }
}
