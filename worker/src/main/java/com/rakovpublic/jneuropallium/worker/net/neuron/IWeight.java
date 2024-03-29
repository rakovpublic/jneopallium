package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.WeightDeserializer;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;

/***
 * Created by Rakovskyi Dmytro on 14.06.2018.
 * This class represents weight/value of connection between neurons.
 */
@JsonDeserialize(using = WeightDeserializer.class)
public interface IWeight<S extends ISignal, C extends ISignal> extends Serializable {

    /**
     * Process signal and change it due to the weight/value
     *
     * @param signal
     * @return signal
     **/
    S process(S signal);

    /**
     * Change the weight due to the change signal.
     *
     * @param signal change signal
     **/
    void changeWeight(C signal);

    /**
     * @return signal class
     **/
    Class<S> getSignalClass();
}
