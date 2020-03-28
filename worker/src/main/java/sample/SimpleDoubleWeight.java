package sample;

import net.neuron.IWeight;
import net.signals.ISignal;

public class SimpleDoubleWeight implements IWeight<SimpleSignal,SimpleChangeWeightSignal> {
    private double weight;

    public SimpleDoubleWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public SimpleSignal process(SimpleSignal signal) {
        return new SimpleSignal(signal.getValue()*weight,signal.getTimeAlive(),signal.getSourceLayerId(),signal.getSourceNeuronId());
    }

    @Override
    public void changeWeight(SimpleChangeWeightSignal signal) {
        weight=weight*signal.getValue();

    }

    @Override
    public Class getSignalClass() {
        return SimpleSignal.class;
    }
}
