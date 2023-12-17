/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.core;

import com.rakovpublic.jneuropallium.worker.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.*;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;

import java.util.HashMap;
import java.util.List;


public interface IInputService {

    List<IResultNeuron> getResults(Integer loop, Long epoch);


    void updateConfiguration(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner, HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies, HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage, HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory, HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses, ISplitInput discriminatorSplitInput, Long nodeTimeOut);

    void inputSourceUpdated(String name);

    void register(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRunsToUpdate);

    void register(InputRegistrationRequest request);

    void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    Boolean isProcessing(String name);

    void uploadDiscriminatorWorkerResult(String name, String nameDiscriminator, HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    ISplitInput getNext(String name);

    boolean hasNextComplete();

    boolean hasPrepared();

    void prepareInputs();

    Boolean runCompleted();

    void prepareResults();

    void nextRun();

    void setLayersMeta(ILayersMeta layersMeta);

    void setRun(Long run);

    void processCallBackFromUpstream(HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    ILayersMeta getLayersMeta();

    void updateLayersMeta(ILayersMeta layersMeta);

    void updateDiscriminators(HashMap<String, ILayersMeta> discriminators);

    boolean hasDiscriminators();

    void prepareDiscriminatorsInputs();

    boolean isDiscriminatorsDone();

    boolean isResultValid();

    ISplitInput getNextDiscriminators(String name);

    void nextRunDiscriminator();
}
