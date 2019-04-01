package net.neuron;

import net.signals.ISignal;

import java.util.List;

public interface ISignalMerger<S extends ISignal> {
    S mergeSignals(List<S> signals);
    String getDescription();
}
