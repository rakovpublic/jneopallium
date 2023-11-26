package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISynapse;

public class SynapseWrapper<N extends ISynapse> extends NeuronSynapse implements ISynapse {
    private N connection;
    private String className;

    public SynapseWrapper(N connection) {
        this.connection = connection;
        this.className = connection.getClass().getCanonicalName();
    }
}
