package net.neuron;

import net.signals.ISignal;

/**
 * Created by Rakovskyi Dmytro on 02.11.2017.
 */
public interface INConnection<S extends ISignal> {
    int getTargetLayerId();

    int getSourceLayerId();

    Long getTargetNeuronId();

    Long getSourceNeuronId();

    String toJSON();

    String getDescription();

    IWeight<S> getWeight();


}
