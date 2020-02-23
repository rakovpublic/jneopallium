package sample;

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

    private boolean activationFunction(){
        return true;
    }
}
