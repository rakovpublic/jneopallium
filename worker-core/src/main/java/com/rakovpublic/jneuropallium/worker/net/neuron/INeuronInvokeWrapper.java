package com.rakovpublic.jneuropallium.worker.net.neuron;
/**
 * Invokes for passing to aws lambda or/and as cuda pre serialization object
 */


import java.io.Serializable;

public interface INeuronInvokeWrapper extends Serializable {
    INeuron getNeuron();

    ISignalStorageCallBack getInput();
}
