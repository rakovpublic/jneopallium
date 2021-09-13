package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.neuron.ISynapse;

public class SynapseWrapper<N extends ISynapse> extends NeuronConnection implements ISynapse {
    private  N connection;
    private String className;

    public SynapseWrapper(N connection) {
        this.connection = connection;
        this.className = connection.getClass().getCanonicalName();
    }
}
