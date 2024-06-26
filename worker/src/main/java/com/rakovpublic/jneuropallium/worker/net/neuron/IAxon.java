package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerMove;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Axon;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/***
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
@JsonDeserialize(as = Axon.class)
public interface IAxon extends Serializable {
    /**
     * Change all connection on new connection map for all signals
     *
     * @param newConnection connection map
     **/
    void resetConnection(HashMap<Class<? extends ISignal>, List<ISynapse>> newConnection);

    /**
     * Add connection for signal to neuron.
     *
     * @param cl         signal class
     * @param connection
     **/
    <S extends ISignal> void putConnection(Class<S> cl, ISynapse<S> connection);

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
    HashMap<ISignal, List<ISynapse>> processSignals(List<ISignal> signals);

    String toJSON();

    /**
     * Translate signal list connection structure to signals which can be stored for next layer processing
     *
     * @param signalConnectionMap HashMap where key is signal and value is list of connection object
     * @return structure key layer id  value  - HashMap key neuron id value list of signals
     **/
    HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getSignalResultStructure(HashMap<ISignal, List<ISynapse>> signalConnectionMap);

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

    /**
     * @return the connection list for each signal class
     */
    HashMap<Class<? extends ISignal>, List<ISynapse>> getConnectionMap();

    /**
     * wraps connections with wrappers for serialization
     */
    void wrapConnections();

    /**
     * unwraps connections for usage
     */
    void unwrapConnections();

    /**
     * @return true if connection wrapped
     */
    Boolean isConnectionsWrapped();

    void moveConnection(LayerMove layerMove, int currentLayer, Long currentNeuronId);

    void setDefaultWeights(HashMap<Class<? extends ISignal>, IWeight> defaultWeights);
}
