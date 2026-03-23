package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.memory.PredictiveNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.ComparisonSignal;
import com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class PredictiveCodingActualProcessor implements ISignalProcessor<SensorySignal, PredictiveNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SensorySignal input, PredictiveNeuron neuron) {
        List<I> results = new ArrayList<>();
        double actual = (input.getRawValues() != null && input.getRawValues().length > 0) ? input.getRawValues()[0] : 0.0;
        double predicted = (neuron.getLastPrediction() != null && neuron.getLastPrediction().length > 0)
                ? neuron.getLastPrediction()[0] : 0.0;
        ComparisonSignal comparison = new ComparisonSignal(predicted, actual, input.getSensorId());
        comparison.setSourceNeuronId(neuron.getId());
        results.add((I) comparison);
        return results;
    }

    @Override public String getDescription() { return "PredictiveCodingActualProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PredictiveCodingActualProcessor.class; }
    @Override public Class<PredictiveNeuron> getNeuronClass() { return PredictiveNeuron.class; }
    @Override public Class<SensorySignal> getSignalClass() { return SensorySignal.class; }
}
