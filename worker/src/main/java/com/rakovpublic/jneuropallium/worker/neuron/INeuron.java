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
    /**
     * Provides access to signal processing chain which describes in which order signals should be processed
     *
     * @return ISignalChain object which provides access to signal processing chain
     **/
    ISignalChain getSignalChain();

    /**
     * Provides access to processor mapping
     *
     * @return map which describe what processor should by invoked for appropriate signal class
     **/
    Map<Class<? extends ISignal>, ISignalProcessor> getProcessorMap();

    /**
     * Provides access to merger mapping. Mergers merge multiple signals into one
     *
     * @return map which describe what merger  should by invoked for appropriate signal class
     **/
    Map<Class<? extends ISignal>, ISignalMerger> getMergerMap();

    /**
     *  @return the number of runs(invokes of this neuron)
     **/
    Long getRun();

    /**
     * set the amount of runs
     * @param run amount of runs
     **/
    void setRun(Long run);

    /**
     * Provides access to signal history
     *
     * @return ISignalHistoryStorage object which provides access to signal history
     **/
    ISignalHistoryStorage getSignalHistory();

    /**
     * set the signal history storage
     * @param signalHistory signal history storage
     **/
    void setSignalHistory(ISignalHistoryStorage signalHistory);

    /**
     * set the neuron id
     * @param id neuron id
     **/
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
    <S extends ISignal, N extends INeuron> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S, N> processor);

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

    /**
     *
     * @return true if neuron has been changed
     * */
    Boolean isChanged();

    /**
     *
     * @param  changed true if neuron has been changed
     * */
    void setChanged(Boolean changed);

    /**
     *
     * @return true if neuron should been deleted
     * */
    Boolean isOnDelete();

    /**
     *
     * @param  onDelete true if neuron should been changed
     * */
    void setOnDelete(Boolean onDelete);


    /**
     *
     * @return map amount of runs should be passed before the IInitInput with this name will be populated next time
     * */
    HashMap<String, Long> getCyclingNeuronInputMapping();

    /**
     *
     * @param cyclingNeuronInputMapping mapping for cycle InitInputs populating
     * */
    void setCyclingNeuronInputMapping(HashMap<String, Long> cyclingNeuronInputMapping);

    /**
     *
     * @return the amount of loops the have been passed after previous Input populating
     * */
    Integer getCurrentLoopAmount();

    void setCurrentLoopAmount(Integer currentLoopCount);

    /**
     * Checks if neuron can process this signal
     * @param signal ISignal signal
     * @return  true if neuron can process this signal
     * */
    boolean canProcess( ISignal signal);

    /**
     * Set the layer of this neuron
     * @param layer
     * */
    void setLayer(ILayer layer);

    /**
     * @return  the layer of this neuron
     * */
    ILayer getLayer();
}
