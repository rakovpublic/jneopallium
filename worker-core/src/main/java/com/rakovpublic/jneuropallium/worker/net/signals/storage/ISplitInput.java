/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IStorageMeta;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The interface incapsulate input for worker run in cluster mode
 */
public interface ISplitInput extends IStorageMeta {
    String getDiscriminatorName();

    void setDiscriminatorName(String name);

    /**
     * @return signal storage
     */
    IInputResolver getInputResolver();

    /**
     * This method save the the input for the processing
     *
     * @param signals neuronId signals list map
     **/
    void saveResults(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals);


    /**
     * This method save the neuron
     *
     * @param neuron neuronToUpdate
     **/
    void saveNeuron(INeuron neuron);

    /**
     * Saves a whole partition worth of neurons. Implementations backed by a remote store should
     * override this to write them in one round trip instead of one per neuron.
     *
     * @param neurons neurons to persist
     */
    default void saveNeurons(List<? extends INeuron> neurons) {
        for (INeuron neuron : neurons) {
            saveNeuron(neuron);
        }
    }

    /**
     * This method set the name of worker where it will be processed
     *
     * @param name
     */
    void setNodeIdentifier(String name);

    /**
     * Creates new empty instance of SplitInput
     *
     * @return slit input
     */
    ISplitInput getNewInstance();

    /**
     * @return the neuron list which should be processed by worker
     */
    List<? extends INeuron> getNeurons();


    /**
     * @return the node name
     */
    String getNodeIdentifier();


    Long getStart();

    Long getEnd();

    void setStart(Long start);

    void setEnd(Long end);

    Integer getLayerId();

    void setLayer(Integer layerId);

    void applyMeta(ILayersMeta layersMeta);

    /**
     * Stamps the assignment with the epoch and loop it belongs to, together with the mapping of
     * input names to cycle neurons. Implementations that resolve their state from a shared store
     * need this because they do not carry the input loading strategy; the ones that ship the
     * whole resolver already know it and can ignore it.
     *
     * @param run                current epoch
     * @param loop               current loop inside the epoch
     * @param cycleNeuronMapping input name to cycle neuron id
     */
    default void applyRunState(Long run, Integer loop, HashMap<String, Long> cycleNeuronMapping) {
    }

    Integer getThreads();


}
