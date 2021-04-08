package sample;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Neuron;

public class SimpleNeuron extends Neuron {
    private double bias;
    private double signal;
    private double biasWeight;
    private Class<?extends INeuron> currentNeuronClass;
    public SimpleNeuron() {
        bias=1;
        biasWeight=1;
        signal=0;
        currentNeuronClass=SimpleNeuron.class;
    }

    public SimpleNeuron(Long neuronId, ISignalChain processingChain) {
        super(neuronId, processingChain);
        bias=1;
        biasWeight=1;
        currentNeuronClass=SimpleNeuron.class;
    }

    @Override
    public void activate() {
        super.activate();
        signal+=bias*biasWeight;
        result.clear();
        if(activationFunction()){
            result.add(new SimpleSignal(signal,1));
        }
        signal=0;
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
    public Class<? extends INeuron> getCurrentNeuronClass() {
        return currentNeuronClass;
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
