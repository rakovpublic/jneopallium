package com.rakovpublic.jneuropallium.ai.neurons.memory;

import com.rakovpublic.jneuropallium.ai.model.InternalForwardModel;
import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Predictive neuron used by PredictiveCodingContextProcessor and PredictiveCodingActualProcessor.
 * Holds an internal forward model and caches the most recent prediction.
 */
public class PredictiveNeuron extends ModulatableNeuron implements IPredictiveNeuron {

    private InternalForwardModel model;
    private double[] lastPrediction;

    public PredictiveNeuron() {
        super();
        this.model = new InternalForwardModel(4);
        this.lastPrediction = new double[]{0.0};
    }

    public PredictiveNeuron(Long neuronId,
                            com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                            Long run,
                            int stateDimensions) {
        super(neuronId, chain, run);
        this.model = new InternalForwardModel(stateDimensions);
        this.lastPrediction = new double[stateDimensions];
    }

    public InternalForwardModel getModel() { return model; }
    public void setModel(InternalForwardModel model) { this.model = model; }

    public double[] getLastPrediction() { return lastPrediction; }
    public void setLastPrediction(double[] lastPrediction) { this.lastPrediction = lastPrediction; }
}
