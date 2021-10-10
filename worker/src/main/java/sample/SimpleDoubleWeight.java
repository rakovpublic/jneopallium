package sample;

import com.rakovpublic.jneuropallium.worker.neuron.IWeight;

public class SimpleDoubleWeight implements IWeight<SimpleSignal, SimpleChangeWeightSignal> {
    private Class<SimpleSignal> signalClass = SimpleSignal.class;

    public SimpleDoubleWeight() {
    }

    private double weight;

    public SimpleDoubleWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public SimpleSignal process(SimpleSignal signal) {
        return new SimpleSignal(signal.getValue() * weight, signal.getTimeAlive(), signal.getSourceLayerId(), signal.getSourceNeuronId());
    }

    @Override
    public void changeWeight(SimpleChangeWeightSignal signal) {
        weight = weight * signal.getValue();

    }

    @Override
    public Class getSignalClass() {
        return signalClass;
    }
}
