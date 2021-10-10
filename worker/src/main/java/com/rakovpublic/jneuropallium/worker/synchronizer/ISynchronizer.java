package com.rakovpublic.jneuropallium.worker.synchronizer;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.io.Serializable;
import java.util.List;

/***
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface ISynchronizer extends Serializable {

    IContext getContext(int nodeId);

    void syncSignal(ISignal signal, int layerId, long neuronId);

    boolean isLayerProcessed(int layerId);

    int getNextLayerId();

    ILayerMeta getNextBatch();

    void syncNeurons(List<? extends INeuron> neurons, int layerId);

    void removeNeuron(int layerId, long neuronId);

    void updateNeuron(INeuron neuron, int layerId);

    void addNeuron(INeuron neuron, long layerId);

    void addLayer(int afterLayerId);

    void putSignals();


}
