package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.model.WorldStateModel;
import com.rakovpublic.jneuropallium.ai.neurons.harm.IConsequenceModelNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ConsequenceQuerySignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.ConsequenceSimulationSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.MotorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class ForwardSimulationProcessor implements ISignalProcessor<ConsequenceQuerySignal, IConsequenceModelNeuron> {

    @Override
    public <I extends ISignal> List<I> process(ConsequenceQuerySignal input, IConsequenceModelNeuron neuron) {
        List<I> results = new ArrayList<>();
        if (input.getCandidateActions() == null) return results;

        for (MotorCommandSignal action : input.getCandidateActions()) {
            // CRITICAL: clone world model — never modify live model
            WorldStateModel simState = neuron.getWorldModel().deepClone();
            String actionType = String.valueOf(action.getEffectorId());
            double[] effectWeights = neuron.getActionEffectWeights().getOrDefault(actionType, new double[]{0.1, 0.1, 0.1, 0.1, 0.1});

            for (int step = 0; step < input.getSimulationHorizon(); step++) {
                double[] humanImpact = new double[5];
                double[] humanState = simState.getHumanStateVector();
                for (int dim = 0; dim < 5; dim++) {
                    double rawImpact = effectWeights[Math.min(dim, effectWeights.length - 1)];
                    // Conservative bias: amplify harmful dimensions when low confidence
                    double confidence = neuron.getActionConfidence(actionType);
                    if (rawImpact < 0 && confidence < 0.5) rawImpact *= 1.5;
                    humanImpact[dim] = rawImpact;
                    humanState[dim] = Math.max(0, humanState[dim] + rawImpact);
                }
                double[] projectedState = simState.getEnvironmentVector().clone();
                ConsequenceSimulationSignal simSignal = new ConsequenceSimulationSignal(
                    input.getActionPlanId(), projectedState, step, humanImpact);
                simSignal.setSourceNeuronId(neuron.getId());
                results.add((I) simSignal);
            }
        }
        return results;
    }

    @Override public String getDescription() { return "ForwardSimulationProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return ForwardSimulationProcessor.class; }
    @Override public Class<IConsequenceModelNeuron> getNeuronClass() { return IConsequenceModelNeuron.class; }
    @Override public Class<ConsequenceQuerySignal> getSignalClass() { return ConsequenceQuerySignal.class; }
}
