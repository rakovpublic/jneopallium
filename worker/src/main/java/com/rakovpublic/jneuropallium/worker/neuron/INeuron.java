package com.rakovpublic.jneuropallium.worker.neuron;


import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalHistoryStorage;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/***
 * Created by Rakovskyi Dmytro on 27.10.2017.
 * This class contains methods for signal processing and represents neuron(perceptron)
 *
 */
public interface INeuron extends Serializable {

    ISignalChain getSignalChain();

    Map<Class<? extends ISignal>, ISignalProcessor> getProcessorMap();
    Map<Class<? extends ISignal>, ISignalMerger> getMergerMap();

    Long getRun();
    void setRun(Long run);

    ISignalHistoryStorage getSignalHistory();
    void setSignalHistory(ISignalHistoryStorage signalHistory);

    void setId(Long id);

    /**
     * Validate neuron on local level
     *
     * @return true if neuron is valid
     **/
    Boolean validate();

    /**
     * Add local validation rule
     *
     * @param rule validation rule
     **/
    void addRule(IRule rule);

    /**
     * Add signals for processing
     *
     * @param signals list
     **/
    void addSignals(List<ISignal> signals);

    /**
     * This method process all signals
     **/
    void processSignals();

    /**
     * Set axon(define where pass results) to neuron
     *
     * @param axon axon object
     **/
    void setAxon(IAxon axon);

   // void setAxon(Axon axon);

    /**
     * Add signal processor to neuron. Define which classes of signals can be processed by neuron.
     *
     * @param clazz     signal class which will be processing by this signal processor
     * @param processor signal processor
     **/
    <S extends ISignal,N extends INeuron> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S,N> processor);

    /**
     * Add signal merger. If signal merger exists for the all the signals of the same will be merged in one before
     * processing.
     *
     * @param clazz  signal class
     * @param merger
     **/
    <S extends ISignal> void addSignalMerger(Class<S> clazz, ISignalMerger<S> merger);

    /**
     * Remove all the signal processors and mergers associated with this signal class.
     *
     * @param clazz signal  class
     **/
    <S extends ISignal> void removeSignalProcessor(Class<S> clazz);

    /**
     * Remove all signal mergers associated with this signal class.
     *
     * @param clazz signal class
     **/
    <S extends ISignal> void removeSignalMerger(Class<S> clazz);

    /**
     * Get the result signals after processing.
     *
     * @return list of signals
     **/
    List<ISignal> getResult();

    /**
     * Check if neuron has been processed.
     *
     * @return true if already processed
     **/
    Boolean hasResult();

    /**
     * Get the neuron id.
     *
     * @return neuron id
     **/
    Long getId();

    /**
     * Get the axon object(which contains the addresses of neurons where should be passed results and weights for signals)
     *
     * @return axon object
     **/
    IAxon getAxon();

    String toJSON();

    /**
     * Set the order of signal processing
     *
     * @param chain signal chain object
     **/
    void setProcessingChain(ISignalChain chain);

    /**
     * Activation function
     */
    void activate();

    Class<? extends INeuron> getCurrentNeuronClass();

    Boolean isChanged();

    void setChanged(Boolean changed);

    Boolean isOnDelete();

    void setOnDelete(Boolean onDelete);

     HashMap<String, Long> getCyclingNeuronInputMapping();

     void setCyclingNeuronInputMapping(HashMap<String, Long> cyclingNeuronInputMapping);

     Integer getCurrentLoopAmount();

     void setCurrentLoopAmount(Integer currentLoopCount) ;

     boolean canProcess( ISignal clazz);


     void setLayer(ILayer layer);

     ILayer getLayer();
}
