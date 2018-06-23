package web.neuron;

import web.signals.ISignal;

import java.util.List;

public interface ISignalMerger<S extends ISignal> {
    S mergeSignals(List<S> signals);
}
