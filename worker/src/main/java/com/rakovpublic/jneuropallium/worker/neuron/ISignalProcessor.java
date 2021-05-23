package com.rakovpublic.jneuropallium.worker.neuron;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.List;

/***
 * Created by Rakovskyi Dmytro on 13.06.2018.
 * This class represents processor for signal
 */
public interface ISignalProcessor<S extends ISignal,K extends  INeuron> extends Serializable {
    /***
     * @param input signal
     * @param neuron object
     * @return list of result signals
     * */
    <I extends ISignal> List<I> process(S input, K neuron);

    /***
     * @return description
     * */
    String getDescription();

    /**
     * check if input signals should be merged before processing
     *
     * @return true if signals should be merged before processing, false if not, null if it depends on existing merger
     * for this class of signals in neuron object(will be merged if merger registered in neuron with the help
     * addSignalMerger method)
     */
    Boolean hasMerger();

    Class<? extends ISignalProcessor> getSignalProcessorClass();

    Class<K> getNeuronClass();

}
