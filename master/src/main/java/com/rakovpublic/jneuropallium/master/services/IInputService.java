package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;

import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

public interface IInputService {
    void inputSourceUpdated(String name);

    void register( IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy,Integer amountOfRunsToUpdate);

    void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    ISplitInput getNext(String name);

    boolean hasNextComplete();

    boolean hasPrepared();

    void prepareInputs();

    Boolean runCompleted();

    SortedSet<? extends IResultNeuron> prepareResults();

    void nextRun();

    void setLayersMeta(ILayersMeta layersMeta);
}
