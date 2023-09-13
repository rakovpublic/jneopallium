package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;

import java.util.HashMap;
import java.util.List;


public interface IInputService {
    void updateConfiguration(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner);

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

    void processCallBackFromUpstream(HashMap<Integer, HashMap<Long, List<ISignal>>> signals);
}
