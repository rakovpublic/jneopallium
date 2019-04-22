package net.neuron;

import net.signals.ISignal;

import java.util.List;
/*
* This class represents logic of merging the same class signals to one signal.
*
* */
public interface ISignalMerger<S extends ISignal> {
    /**
    * @param signals list of signals
    * @return  signal
    * */
    S mergeSignals(List<S> signals);

    /**
     * @return description
     * */
    String getDescription();
}
