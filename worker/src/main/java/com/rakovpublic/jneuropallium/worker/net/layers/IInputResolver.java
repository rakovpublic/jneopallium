package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;

import java.util.HashMap;
import java.util.List;

/**
 * This object represents input managing logic
 *
 *
 * */
public interface IInputResolver {

    /**
     * This method add input source
     * @param iInputSource describes where get input
     * @param isMandatory check if availability this input is mandatory to make next run
     * @param initStrategy describes how to populate this input to neurons(which layers and neurons should get the input)
     * @param amountOfRuns describes how often this input should be populated
     * */
    void registerInput(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns);

    /**
     * @return signals storage
     * */
    ISignalsPersistStorage getSignalPersistStorage();

    /**
     * @return signals history
     * */
    ISignalHistoryStorage getSignalsHistoryStorage();

    /**
     * @return mapping for service neuron on Layer id Long.MIN
     * */
    HashMap<String, Long> getCycleNeuronAddressMapping();

    /**
     * @return current loop inside run
     * */
    Integer getCurrentLoop();

    /**
     * @return amount of runs from the beginning of the processing
     * */
    Long getCurrentRun();

    /**
     * save signals from signal storage to history
     * */
    void saveHistory();

    /**
     * populates new input to signal storage
     * */
    void populateInput();

    /**
     *@return  get expected  results
     * */
    HashMap<String, List<IResultSignal>> getDesiredResult();

    /**
     * send call back to the input
     * @param name input name
     * @param signals
     * */
    void sendCallBack(String name, List<ISignal> signals);
}
