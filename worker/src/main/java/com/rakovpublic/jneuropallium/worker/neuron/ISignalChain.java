package com.rakovpublic.jneuropallium.worker.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.SignalChainDeserializer;
import sample.SimpleSignalChain;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents order of signal processing
 */

//TODO: refactor with StdConverter
@JsonDeserialize(using= SignalChainDeserializer.class)
public interface ISignalChain extends Serializable {

    /**
     * @return list of signal classes which represents order of signals processing
     **/
    List<Class<? extends ISignal>> getProcessingChain();

    /**
     * @return description
     **/
    String getDescription();
}
