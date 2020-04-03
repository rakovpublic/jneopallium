package sample;

import net.neuron.INeuron;

public class SimpleResultNeuron extends SimpleNeuron {
    private Class<? extends INeuron> currentNeuronClass=SimpleResultNeuron.class;
    @Override
    public Class<? extends INeuron> getCurrentNeuronClass() {
        return super.getCurrentNeuronClass();
    }

    @Override
    public void activate() {
        super.activate();
        result.clear();
        result.add(new SimpleResult());
    }
}
