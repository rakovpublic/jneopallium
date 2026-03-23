package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.memory.PredictiveNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.WorkingMemoryReadSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class PredictiveCodingContextProcessor implements ISignalProcessor<WorkingMemoryReadSignal, PredictiveNeuron> {

    @Override
    public <I extends ISignal> List<I> process(WorkingMemoryReadSignal input, PredictiveNeuron neuron) {
        // Update model state from context
        double[] stateUpdate = new double[]{1.0}; // simplified
        neuron.getModel().updateState(stateUpdate);
        double[] currentState = neuron.getModel().getState();
        double[] prediction = neuron.getModel().predict(currentState);
        neuron.setLastPrediction(prediction);
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "PredictiveCodingContextProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return PredictiveCodingContextProcessor.class; }
    @Override public Class<PredictiveNeuron> getNeuronClass() { return PredictiveNeuron.class; }
    @Override public Class<WorkingMemoryReadSignal> getSignalClass() { return WorkingMemoryReadSignal.class; }
}
