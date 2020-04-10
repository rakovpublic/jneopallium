package net.neuron;

import net.signals.IResultSignal;

public interface IResultNeuron<K extends IResultSignal> extends INeuron {
    K getFinalResult();
}
