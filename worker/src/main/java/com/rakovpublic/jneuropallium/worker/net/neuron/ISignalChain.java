package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.SignalChainDeserializer;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.List;

/**
 * This class represents order of signal processing
 */

@JsonDeserialize(using = SignalChainDeserializer.class)
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
