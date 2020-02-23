package sample;

import net.neuron.INeuron;
import net.neuron.ISignalChain;
import net.neuron.impl.Neuron;

public class SimpleNeuron extends Neuron {
    private double bias;
    private double signal;
    private double biasWeight;
    public SimpleNeuron() {
        bias=1;
        biasWeight=1;
    }

    public SimpleNeuron(Long neuronId, ISignalChain processingChain) {
        super(neuronId, processingChain);
        bias=1;
        biasWeight=1;
    }

    @Override
    public void activate() {
        super.activate();
        signal+=bias*biasWeight;
        result.clear();
        if(activationFunction()){
            result.add(new SimpleSignal(signal,1));
        }
    }

    public double getBias() {
        return bias;
    }

    public void setBias(double bias) {
        this.bias = bias;
    }

    public double getBiasWeight() {
        return biasWeight;
    }

    public void setBiasWeight(double biasWeight) {
        this.biasWeight = biasWeight;
    }

    private boolean activationFunction(){
        return true;
    }

    @Override
    public Class<? extends INeuron> getCurrentClass() {
        return SimpleNeuron.class;
    }
}
