package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.enums.NeuromodulatorType;
import com.rakovpublic.jneuropallium.ai.model.AlternativeAction;
import com.rakovpublic.jneuropallium.ai.neurons.harm.HarmGateNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.*;
import com.rakovpublic.jneuropallium.ai.signals.slow.NeuromodulatorSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

public class HarmResultProcessor implements ISignalProcessor<HarmAssessmentSignal, HarmGateNeuron> {

    @Override
    public <I extends ISignal> List<I> process(HarmAssessmentSignal input, HarmGateNeuron neuron) {
        List<I> results = new ArrayList<>();
        String planId = input.getActionPlanId();
        // Cache the assessment
        neuron.cacheAssessment(planId, input);

        // Always emit transparency log
        TransparencyLogSignal log = new TransparencyLogSignal(planId,
            "verdict:" + input.getVerdict(),
            input.getTriggeringConditions(), input.getVerdict(), System.currentTimeMillis());
        log.setSourceNeuronId(neuron.getId());
        results.add((I) log);

        HarmVerdict effectiveVerdict = input.getVerdict();

        // UNCERTAIN with confidence below threshold -> treat as HARMFUL after retry
        if (effectiveVerdict == HarmVerdict.UNCERTAIN) {
            if (input.getConfidence() < neuron.getUncertaintyThreshold()) {
                int retryCount = neuron.getUncertaintyRetryCount().getOrDefault(planId, 0);
                if (retryCount >= 1) {
                    // Precautionary principle: UNCERTAIN after retry = HARMFUL
                    effectiveVerdict = HarmVerdict.HARMFUL;
                } else {
                    neuron.getUncertaintyRetryCount().put(planId, retryCount + 1);
                    return results; // hold for one tick
                }
            }
        }

        Queue<MotorCommandSignal> pendingQueue = neuron.getPendingQueue();
        MotorCommandSignal pending = null;
        for (MotorCommandSignal cmd : pendingQueue) {
            if (planId.equals(cmd.getActionPlanId())) { pending = cmd; break; }
        }
        if (pending != null) pendingQueue.remove(pending);

        switch (effectiveVerdict) {
            case SAFE:
                if (pending != null) {
                    MotorCommandSignal released = pending.copySignal();
                    released.setExecute(true);
                    results.add((I) released);
                }
                break;
            case HARMFUL:
                HarmVetoSignal veto = new HarmVetoSignal(planId, "Harm detected: " + String.join(", ", input.getTriggeringConditions()),
                    HarmVerdict.HARMFUL, new AlternativeAction[0]);
                veto.setSourceNeuronId(neuron.getId());
                results.add((I) veto);
                break;
            case CATASTROPHIC:
                HarmVetoSignal catVeto = new HarmVetoSignal(planId, "CATASTROPHIC harm detected",
                    HarmVerdict.CATASTROPHIC, new AlternativeAction[0]);
                catVeto.setSourceNeuronId(neuron.getId());
                results.add((I) catVeto);
                NeuromodulatorSignal serotonin = new NeuromodulatorSignal(NeuromodulatorType.SEROTONIN, 1.0, "broadcast");
                serotonin.setSourceNeuronId(neuron.getId());
                results.add((I) serotonin);
                break;
        }
        return results;
    }

    @Override public String getDescription() { return "HarmResultProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return HarmResultProcessor.class; }
    @Override public Class<HarmGateNeuron> getNeuronClass() { return HarmGateNeuron.class; }
    @Override public Class<HarmAssessmentSignal> getSignalClass() { return HarmAssessmentSignal.class; }
}
