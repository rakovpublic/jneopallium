package sample;

import net.neuron.IWeight;
import net.signals.ISignal;

public class SimpleDoubleWeight implements IWeight<SimpleSignal,SimpleChangeWeightSignal> {


    @Override
    public SimpleSignal process(SimpleSignal signal) {
        return null;
    }

    @Override
    public void changeWeight(SimpleChangeWeightSignal signal) {

    }

    @Override
    public Class getSignalClass() {
        return null;
    }
}
