package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

/**
 * Framework-level processor that routes a {@link MovementObservationSignal} to the movement policy.
 * It returns the {@link MovementDecisionSignal} (carrying the {@link MotorCommand}) and, when the
 * previous decision could be scored from the new observation, the auto-reinforcement result too.
 */
public class MovementPolicyProcessor implements ISignalProcessor<MovementObservationSignal, IMovementPolicyNeuron> {
    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MovementObservationSignal input, IMovementPolicyNeuron neuron) {
        List<I> result = new ArrayList<>();
        MovementPolicyNetwork.DecisionOutcome outcome = neuron.getNetwork().decide(input);
        result.add((I) outcome.getDecision());
        if (outcome.getAutoReinforcement() != null && outcome.getAutoReinforcement().isApplied()) {
            result.add((I) outcome.getAutoReinforcement());
        }
        return result;
    }

    @Override public String getDescription() { return "autonomous movement policy decision processor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MovementPolicyProcessor.class; }
    @Override public Class<IMovementPolicyNeuron> getNeuronClass() { return IMovementPolicyNeuron.class; }
    @Override public Class<MovementObservationSignal> getSignalClass() { return MovementObservationSignal.class; }
}
