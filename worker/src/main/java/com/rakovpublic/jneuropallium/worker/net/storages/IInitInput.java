package com.rakovpublic.jneuropallium.worker.net.storages;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;
/**
 * This interface represents part of the input for neuron net
 *
 * */
@JsonDeserialize(using = InitInputDeserializer.class)
public interface IInitInput {

    /**
     * @return the signals for this input
     * */
    List<IInputSignal> readSignals();

    /**
     * @return the name of this input
     * */
    String getName();

    /**
     * @return the call back input if input comes from other neuron net and it supports callback studying, null if not
     * */
    INeuronNetInput getNeuronNetInput();

    /**
     * @return desired result for this input
     * */
    HashMap<String, List<IResultSignal>> getDesiredResults();
}
