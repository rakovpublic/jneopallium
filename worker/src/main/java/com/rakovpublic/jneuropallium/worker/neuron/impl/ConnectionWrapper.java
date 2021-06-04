package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.neuron.INConnection;

public class ConnectionWrapper<N extends INConnection> extends NeuronConnection implements INConnection {
    private  N connection;
    private String className;

    public ConnectionWrapper(N connection) {
        this.connection = connection;
        this.className = connection.getClass().getCanonicalName();
    }
}
