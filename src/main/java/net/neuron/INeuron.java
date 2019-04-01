package net.neuron;


import net.signals.ISignal;

import java.util.List;

/**
 * Created by Rakovskyi Dmytro on 27.10.2017.
 */
public interface INeuron {
    Boolean validate();

    void addRule(IRule rule);

    void addSignals(List<ISignal> signals);

    void processSignals();


    void setAxon(IAxon axon);

    <S extends ISignal> void addSignalProcessor(Class<S> clazz, ISignalProcessor<S> processor);

    <S extends ISignal> void addSignalMerger(Class<S> clazz, ISignalMerger<S> merger);

    <S extends ISignal> void removeSignalProcessor(Class<S> clazz);

    <S extends ISignal> void removeSignalMerger(Class<S> clazz);

    List<ISignal> getResult();

    Boolean hasResult();

    Long getId();

    IAxon getAxon();

    String toJSON();

    void setProcessingChain(ISignalChain chain);

}
