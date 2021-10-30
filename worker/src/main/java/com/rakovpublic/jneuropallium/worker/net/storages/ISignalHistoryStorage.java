package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

/**
 * This interface represents the history of signals for each neuron. It can be useful for studying
 * */
public interface ISignalHistoryStorage {
    /**
     * This method return the signals which have got neuron on specific run
     * @param nRun the id of run for which will be extracted history
     * @param forTarget neuron address for which will be extracted history
     * @return the list of signals which was on input for neuron on run
     * */
    List<ISignal> getSourceSignalsForRun(Long nRun, NeuronAddress forTarget);

    /**
     * This method saves signals for history
     * @param history signals
     * @param run
     * */
    void save(HashMap<Integer, HashMap<Long, List<ISignal>>> history, Long run);
}
