package net.neuron;

import net.signals.IResultSignal;

public interface IResultNeuron extends INeuron {
    <K extends IResultSignal> K getFinalResult();
}
