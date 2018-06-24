package web.neuron;

import web.signals.ISignal;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface IAxon {
    <S extends ISignal> void putConnection(Class<S> cl, INConnection connection, IWeight<S> weight);

    HashMap<Class<? extends ISignal>, HashMap<INConnection, IWeight>> getConnectedNeurons();

    void rebuildConnections(HashMap<Class<? extends ISignal>, HashMap<INConnection, IWeight>> connections);

    HashMap<ISignal, List<INConnection>> processSignal(List<ISignal> signal);

    String toJSON();
}
