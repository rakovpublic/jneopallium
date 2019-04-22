package net.neuron;

import net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface IAxon {
    <S extends ISignal> void putConnection(Class<S> cl, INConnection<S> connection);

    void cleanConnections();

    HashMap<ISignal, List<INConnection>> processSignals(List<ISignal> signals);

    String toJSON();

    void destroyConnection(int layerId, Long neuronId, Class<? extends ISignal> clazz);

    void changeWeight(int layerId, Long neuronId, ISignal signal);
    void changeWeight(int layerId, Long neuronId, Class<? extends ISignal> clazz,ISignal signal);
    void changeWeight(ISignal signal);
}
