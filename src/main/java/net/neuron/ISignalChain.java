package net.neuron;

import net.signals.ISignal;

import java.util.List;

public interface ISignalChain {
    List<Class<? extends ISignal>> getProcessingChain();
    String getDescription();
}
