package web.neuron.impl;

import web.neuron.IAxon;
import web.neuron.INConnection;
import web.neuron.IWeight;
import web.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public class Axon implements IAxon {
    @Override
    public <S extends ISignal> void putConnection(Class<S> cl, INConnection connection, IWeight<S> weight) {

    }

    @Override
    public HashMap<Class<? extends ISignal>, HashMap<INConnection, IWeight>> getConnectedNeurons() {
        return null;
    }

    @Override
    public void rebuildConnections(HashMap<Class<? extends ISignal>, HashMap<INConnection, IWeight>> connections) {

    }

    @Override
    public HashMap<ISignal, List<INConnection>> processSignal(List<ISignal> signal) {
        return null;
    }

    @Override
    public String toJSON() {
        return null;
    }
}
