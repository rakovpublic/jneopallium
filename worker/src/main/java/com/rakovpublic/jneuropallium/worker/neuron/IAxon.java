package com.rakovpublic.jneuropallium.worker.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.neuron.impl.Axon;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/***
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
@JsonDeserialize(as= Axon.class)
public interface IAxon extends Serializable {
    /**
     * Change all connection on new connection map for all signals
     *
     * @param newConnection connection map
     **/
    void resetConnection(HashMap<Class<? extends ISignal>, List<INConnection>> newConnection);

    /**
     * Add connection for signal to neuron.
     *
     * @param cl         signal class
     * @param connection
     **/
    <S extends ISignal> void putConnection(Class<S> cl, INConnection<S> connection);

    /**
     * Clean all connections from axon
     **/
    void cleanConnections();

    /**
     * Calculate signals for passing to neurons
     *
     * @param signals list
     * @return structure which contains signal object as key and connection list
     **/
    HashMap<ISignal, List<INConnection>> processSignals(List<ISignal> signals);

    String toJSON();

    /**
     * Translate signal list connection structure to signals which can be stored for next layer processing
     *
     * @param signalConnectionMap HashMap where key is signal and value is list of connection object
     * @return structure key layer id  value  - HashMap key neuron id value list of signals
     **/
    HashMap<Integer, HashMap<Long, List<ISignal>>> getSignalResultStructure(HashMap<ISignal, List<INConnection>> signalConnectionMap);

    /**
     * Destroy connection
     *
     * @param layerId  layer id
     * @param neuronId neuron id
     * @param clazz    signal class
     **/
    void destroyConnection(int layerId, Long neuronId, Class<? extends ISignal> clazz);

    /**
     * Change weight
     *
     * @param layerId  layer id
     * @param neuronId neuron id
     * @param signal   change signal
     **/
    void changeAllWeightsForNeuron(int layerId, Long neuronId, ISignal signal);

    /**
     * Change weight for signal class
     *
     * @param layerId  layer id
     * @param neuronId neuron id
     * @param clazz    signal class for which should be changed weight
     * @param signal   change signal
     **/
    void changeAllWeightsForNeuronAndSignal(int layerId, Long neuronId, Class<? extends ISignal> clazz, ISignal signal);

    /**
     * Change all weights
     *
     * @param signal change signal
     **/
    void changeAllWeights(ISignal signal);

    HashMap<Class<? extends ISignal>, List<INConnection>> getConnectionMap();

    void wrapConnections();

    void unwrapConnections();

    Boolean isConnectionsWrapped();
}
