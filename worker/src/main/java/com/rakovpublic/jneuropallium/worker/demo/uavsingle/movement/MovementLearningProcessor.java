package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

/**
 * Framework-level processor that applies a {@link MovementReinforcementSignal} to the movement
 * policy. Mirrors {@code RecognitionLearningProcessor}.
 */
public class MovementLearningProcessor implements ISignalProcessor<MovementReinforcementSignal, IMovementLearningNeuron> {
    @Override
    @SuppressWarnings("unchecked")
    public <I extends ISignal> List<I> process(MovementReinforcementSignal input, IMovementLearningNeuron neuron) {
        List<I> result = new ArrayList<>();
        result.add((I) neuron.applyReward(input));
        return result;
    }

    @Override public String getDescription() { return "movement reinforcement feedback processor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return MovementLearningProcessor.class; }
    @Override public Class<IMovementLearningNeuron> getNeuronClass() { return IMovementLearningNeuron.class; }
    @Override public Class<MovementReinforcementSignal> getSignalClass() { return MovementReinforcementSignal.class; }
}
