package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

/**
 * The interface incapsulate input for worker run in cluster mode
 */
public interface ISplitInput extends IStorageMeta {

    /**
     * @return signal storage
     */
    IInputResolver getInputResolver();

    /**
     * This method save the the input for the processing
     *
     * @param signals neuronId signals list map
     **/
    void saveResults(HashMap<Integer, HashMap<Long, List<ISignal>>> signals);


    /**
     * This method save the neuron
     *
     * @param neuron neuronToUpdate
     **/
    void saveNeuron(INeuron neuron);

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


}
