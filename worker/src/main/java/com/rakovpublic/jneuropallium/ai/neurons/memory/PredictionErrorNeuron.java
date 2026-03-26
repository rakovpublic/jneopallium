package com.rakovpublic.jneuropallium.ai.neurons.memory;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Reward prediction error neuron used by RewardPredictionErrorProcessor.
 * Emits dopamine on positive surprise and serotonin on negative surprise.
 */
public class PredictionErrorNeuron extends ModulatableNeuron {

    private double thetaPositive;
    private double thetaNegative;
    private String planningNeuronId;

    public PredictionErrorNeuron() {
        super();
        this.thetaPositive = 0.1;
        this.thetaNegative = -0.1;
        this.planningNeuronId = "0";
    }

    public PredictionErrorNeuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 double thetaPositive,
                                 double thetaNegative,
                                 String planningNeuronId) {
        super(neuronId, chain, run);
        this.thetaPositive = thetaPositive;
        this.thetaNegative = thetaNegative;
        this.planningNeuronId = planningNeuronId;
    }

    public double getThetaPositive() { return thetaPositive; }
    public void setThetaPositive(double thetaPositive) { this.thetaPositive = thetaPositive; }

    public double getThetaNegative() { return thetaNegative; }
    public void setThetaNegative(double thetaNegative) { this.thetaNegative = thetaNegative; }

    public String getPlanningNeuronId() { return planningNeuronId; }
    public void setPlanningNeuronId(String planningNeuronId) { this.planningNeuronId = planningNeuronId; }
}
