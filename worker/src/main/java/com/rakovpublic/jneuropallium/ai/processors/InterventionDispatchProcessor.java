package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.InterventionType;
import com.rakovpublic.jneuropallium.ai.enums.LoopType;
import com.rakovpublic.jneuropallium.ai.enums.NeuromodulatorType;
import com.rakovpublic.jneuropallium.ai.model.ActiveIntervention;
import com.rakovpublic.jneuropallium.ai.neurons.loop.LoopCircuitBreakerNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.loop.ILoopCircuitBreakerNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.LoopAlertSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.LoopInterventionSignal;
import com.rakovpublic.jneuropallium.ai.signals.slow.LoopRecoverySignal;
import com.rakovpublic.jneuropallium.ai.signals.slow.NeuromodulatorSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class InterventionDispatchProcessor implements ISignalProcessor<LoopAlertSignal, ILoopCircuitBreakerNeuron> {

    @Override
    public <I extends ISignal> List<I> process(LoopAlertSignal input, ILoopCircuitBreakerNeuron neuron) {
        List<I> results = new ArrayList<>();
        double severity = input.getSeverity();
        String region = input.getRegionId();
        String interventionId = UUID.randomUUID().toString();

        // Check escalation
        int histCount = neuron.getInterventionHistory().getOrDefault(region, 0);
        neuron.getInterventionHistory().put(region, histCount + 1);

        InterventionType type;
        double param;
        int duration;

        if (histCount >= neuron.getMaxInterventions()) {
            type = InterventionType.RESET_REGION;
            param = 0.0; duration = 30;
        } else if (input.getType() == LoopType.GOAL_ATTRACTOR) {
            type = InterventionType.SCALE_WEIGHTS;
            param = 0.5; duration = 50;
        } else if (severity < 0.30) {
            type = InterventionType.SCALE_WEIGHTS;
            param = 0.7; duration = 50;
        } else if (severity < 0.60) {
            type = InterventionType.INJECT_INHIBITION;
            param = 0.5; duration = 30;
        } else if (severity < 0.85) {
            type = InterventionType.BREAK_CONNECTION;
            param = 0.0; duration = 20;
        } else {
            type = InterventionType.QUARANTINE_NEURON;
            param = 0.0; duration = 15;
            NeuromodulatorSignal gaba = new NeuromodulatorSignal(NeuromodulatorType.GABA, 1.0, region);
            gaba.setSourceNeuronId(neuron.getId());
            results.add((I) gaba);
        }

        // CRITICAL: durationTicks must always be > 0
        if (duration <= 0) duration = 1;

        ActiveIntervention intervention = new ActiveIntervention(interventionId, type, input.getRegionId(), param, duration);
        neuron.getActiveInterventions().put(interventionId, intervention);

        LoopInterventionSignal interventionSignal = new LoopInterventionSignal(type, region, param, duration);
        interventionSignal.setSourceNeuronId(neuron.getId());
        results.add((I) interventionSignal);

        // CRITICAL: always schedule LoopRecoverySignal
        LoopRecoverySignal recovery = new LoopRecoverySignal(region, interventionId, false);
        recovery.setSourceNeuronId(neuron.getId());
        recovery.timeAlive = duration;
        results.add((I) recovery);

        return results;
    }

    @Override public String getDescription() { return "InterventionDispatchProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return InterventionDispatchProcessor.class; }
    @Override public Class<ILoopCircuitBreakerNeuron> getNeuronClass() { return ILoopCircuitBreakerNeuron.class; }
    @Override public Class<LoopAlertSignal> getSignalClass() { return LoopAlertSignal.class; }
}
