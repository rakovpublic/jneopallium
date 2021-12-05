package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;

import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

public interface IInputService {
    void inputSourceUpdated(String name);

    void register(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRunsToUpdate);

    void register(InputRegistrationRequest request);

    void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    ISplitInput getNext(String name);

    boolean hasNextComplete();

    boolean hasPrepared();

    void prepareInputs();

    Boolean runCompleted();

    List<? extends IResultNeuron> prepareResults();

    void nextRun();

    void setLayersMeta(ILayersMeta layersMeta);

    void setRun(Long run);
}
