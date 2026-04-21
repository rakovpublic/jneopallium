package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.enums.NeuromodulatorType;
import com.rakovpublic.jneuropallium.ai.neurons.memory.PredictionErrorNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.memory.IPredictionErrorNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ComparisonSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.ErrorSignal;
import com.rakovpublic.jneuropallium.ai.signals.slow.NeuromodulatorSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class RewardPredictionErrorProcessor implements ISignalProcessor<ComparisonSignal, IPredictionErrorNeuron> {

    @Override
    public <I extends ISignal> List<I> process(ComparisonSignal input, IPredictionErrorNeuron neuron) {
        List<I> results = new ArrayList<>();
        double delta = input.getActual() - input.getPredicted();
        if (delta > neuron.getThetaPositive()) {
            NeuromodulatorSignal dopamine = new NeuromodulatorSignal(NeuromodulatorType.DOPAMINE, delta, "broadcast");
            dopamine.setSourceNeuronId(neuron.getId());
            results.add((I) dopamine);
        }
        if (delta < neuron.getThetaNegative()) {
            NeuromodulatorSignal serotonin = new NeuromodulatorSignal(NeuromodulatorType.SEROTONIN, Math.abs(delta), "broadcast");
            serotonin.setSourceNeuronId(neuron.getId());
            results.add((I) serotonin);
        }
        ErrorSignal err = new ErrorSignal(delta, neuron.getPlanningNeuronId(), false);
        err.setSourceNeuronId(neuron.getId());
        results.add((I) err);
        return results;
    }

    @Override public String getDescription() { return "RewardPredictionErrorProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return RewardPredictionErrorProcessor.class; }
    @Override public Class<IPredictionErrorNeuron> getNeuronClass() { return IPredictionErrorNeuron.class; }
    @Override public Class<ComparisonSignal> getSignalClass() { return ComparisonSignal.class; }
}
