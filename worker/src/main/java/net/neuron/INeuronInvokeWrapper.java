package net.neuron;
/**
 * Invokes for passing to aws lambda or/and as cuda pre serialization object
 */

import net.storages.IInputMeta;

import java.io.Serializable;

public interface INeuronInvokeWrapper extends Serializable {
    INeuron getNeuron();

    IInputMeta getInput();
}
