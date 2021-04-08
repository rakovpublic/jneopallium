package com.rakovpublic.jneuropallium.worker.net.study;

import com.rakovpublic.jneuropallium.worker.neuron.INConnection;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

/**
 * Request for rebuilding neuron connections
 */
public interface IStudyingRequest extends Serializable {
    int getLayerId();

    Long getNeuronId();

    HashMap<Class<? extends ISignal>, List<INConnection>> getNewConnections();

    String toJSON();
}
