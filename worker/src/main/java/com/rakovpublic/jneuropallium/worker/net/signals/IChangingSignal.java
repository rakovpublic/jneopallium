package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.neuron.INeuron;

public interface IChangingSignal<K, T extends INeuron> extends ISignal<K> {

    Class<T> getTargetNeuronClass();

    void changeNeuron(T neuron);

    boolean canProcess(Class<T> cl);

}
