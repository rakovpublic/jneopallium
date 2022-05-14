package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

import java.util.HashMap;
import java.util.List;

/**
 * The interface incapsulate input for worker run in cluster mode
 * */
public interface ISplitInput extends IStorageMeta {

    /**
     * @return signal storage
     * */
    ISignalStorage readInputs();

    /**
     * This method save the the input for the processing
     * @param signals neuronId signals list map
     **/
    void saveResults(HashMap<Integer,HashMap<Long, List<ISignal>>> signals);

    /**
     * This method set the name of worker where it will be processed
     * @param name
     * */
    void setNodeIdentifier(String name);

    /**
     * Creates new empty instance of SplitInput
     * @return slit input
     * */
    ISplitInput getNewInstance();

    /**
     * @return the neuron list which should be processed by worker
     * */
    List<? extends INeuron> getNeurons();

    /**
     * Set the mapping for service neurons on Long.MIN layerid for example for cycling
     * @param neuronInputNameMapping
     * */
    void setServiceInputsMap(HashMap<String, Long> neuronInputNameMapping);

    /**
     * @return service neuron mapping on Long.MIN layerId
     * */
    HashMap<String, Long> getServiceInputsMap();

    /**
     * @return amount of runs which passed after last input population
     * */
    Integer getCurrentLoopCount();

    void setCurrentLoopCount(Integer currentLoopCount);

    /**
     * @return the node name
     * */
    String getNodeIdentifier();

    /**
     *
     * @param run amount of input populations -1 have passed after neuron net started
     * */
    void setRun(Long run);

    /**
     *
     * @return the amount of input population -1 passed after neuron net started
     * */
    Long getRun();


    Long getStart();

    Long getEnd();

    void setStart(Long start);

    void setEnd(Long end);

    Integer getLayerId();

    void setLayer(Integer layerId);


}
