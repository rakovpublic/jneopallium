package net.signals;

import net.neuron.INeuron;

public interface IChangingSignal<K,T extends INeuron> extends ISignal<K> {

    Class<T> getTargetNeuronClass();
    void changeNeuron(T neuron);
    boolean canProcess(Class<T> cl);

}
