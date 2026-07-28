/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.core;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.model.DiscriminatorStatus;
import com.rakovpublic.jneuropallium.worker.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.model.NodeMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.DiscriminatorResultLayer;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.ResultLayerHolder;
import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.signals.*;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class InputService implements IInputService {
    private static final Logger logger = LogManager.getLogger(InputService.class);
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<String, NodeMeta> nodeMetas;
    private HashMap<IInitInput, InputInitStrategy> inputs;
    private ISignalsPersistStorage signalsPersist;
    private ILayersMeta layersMeta;
    private List<ISplitInput> preparedInputs;
    private ISplitInput splitInput;
    private Integer partitions;
    private IInputLoadingStrategy runningStrategy;
    private ISignalHistoryStorage signalHistoryStorage;
    private Long run;
    private Boolean runFlag;
    private IResultLayerRunner resultLayerRunner;
    private HashMap<String, ILayersMeta> discriminators;
    private List<DiscriminatorStatus> discriminatorStatuses;
    private List<ISplitInput> preparedDiscriminatorsInputs;
    private HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies;
    private HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage;
    private HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory;
    private HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses;
    private ISplitInput discriminatorSplitInput;
    private final HashMap<Long, HashMap<Integer, List<IResultNeuron>>> results;
    private Long nodeTimeOut;
    private final ResultLayerHolder resultLayerHolder;
    private Long runOnceIn = 0l;


    public InputService(Long runOnceIn, ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner, HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies, HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage, HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory, HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses, ISplitInput discriminatorSplitInput, Long nodeTimeOut, ResultLayerHolder resultLayerHolder) {
        this.signalsPersist = signalsPersist;
        this.layersMeta = layersMeta;
        this.inputDiscriminatorStatuses = inputDiscriminatorStatuses;
        this.discriminatorSplitInput = discriminatorSplitInput;
        this.nodeTimeOut = nodeTimeOut;
        this.preparedInputs = new ArrayList<>();
        this.splitInput = splitInput;
        this.partitions = partitions;
        this.runningStrategy = runningStrategy;
        this.nodeMetas = new HashMap<>();
        this.inputs = new HashMap<>();
        this.inputStatuses = new HashMap<>();
        this.signalHistoryStorage = signalHistoryStorage;
        this.discriminators = new HashMap<>();
        preparedDiscriminatorsInputs = new LinkedList<>();
        discriminatorStatuses = new LinkedList<>();
        runFlag = false;
        this.resultLayerRunner = resultLayerRunner;
        this.discriminatorsLoadingStrategies = discriminatorsLoadingStrategies;
        this.discriminatorsSignalStorage = discriminatorsSignalStorage;
        this.discriminatorsSignalStorageHistory = discriminatorsSignalStorageHistory;
        this.results = new HashMap<>();
        this.resultLayerHolder = resultLayerHolder;
        this.runOnceIn =runOnceIn;
        this.run = 0l;
    }


    @Override
    public List<IResultNeuron> getResults(Integer loop, Long epoch) {
        HashMap<Integer, List<IResultNeuron>> epochResults = results.get(epoch);
        if (epochResults == null) {
            return null;
        }
        return epochResults.get(loop);
    }

    @Override
    public void updateConfiguration(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner, HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies, HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage, HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory, HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses, ISplitInput discriminatorSplitInput, Long nodeTimeOut) {
        this.signalsPersist = signalsPersist;
        this.layersMeta = layersMeta;
        this.nodeTimeOut = nodeTimeOut;
        this.preparedInputs = new ArrayList<>();
        this.splitInput = splitInput;
        this.partitions = partitions;
        this.runningStrategy = runningStrategy;
        this.nodeMetas = new HashMap<>();
        this.inputs = new HashMap<>();
        this.inputStatuses = new HashMap<>();
        this.signalHistoryStorage = signalHistoryStorage;
        runFlag = false;
        this.resultLayerRunner = resultLayerRunner;
        this.discriminators = new HashMap<>();
        preparedDiscriminatorsInputs = new LinkedList<>();
        discriminatorStatuses = new LinkedList<>();
        this.discriminatorsLoadingStrategies = discriminatorsLoadingStrategies;
        this.discriminatorsSignalStorage = discriminatorsSignalStorage;
        this.discriminatorsSignalStorageHistory = discriminatorsSignalStorageHistory;
        this.inputDiscriminatorStatuses = inputDiscriminatorStatuses;
        this.discriminatorSplitInput = discriminatorSplitInput;
        if (this.run == null) {
            this.run = 0l;
        }
    }

    @Override
    public void inputSourceUpdated(String name) {
        inputStatuses.get(name).setStatus(true);
    }

    @Override
    public synchronized void register(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {
        inputStatuses.put(iInputSource, new InputStatusMeta(true, isMandatory, iInputSource.getName()));
        inputs.put(iInputSource, initStrategy);
        // The loading strategy owns the cycle layer and the input to strategy mapping, so it has
        // to learn about the new input; without this the input is registered but never read.
        runningStrategy.updateInputs(inputStatuses, inputs);
    }

    @Override
    public void register(InputRegistrationRequest request) {
        String inputClass = request.getiInputSourceClass();
        ObjectMapper mapper = new ObjectMapper();
        IInitInput initInput = null;
        try {
            initInput = (IInitInput) mapper.readValue(request.getiInputSourceJson(), Class.forName(inputClass));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("cannot parse initinput  " + request.getiInputSourceJson(), e);
        }
        String loadingStrategyClass = request.getInitStrategyClass();
        InputInitStrategy inputLoadingStrategy = null;
        try {
            inputLoadingStrategy = (InputInitStrategy) mapper.readValue(request.getInitStrategy(), Class.forName(loadingStrategyClass));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("cannot parse initinput  " + request.getiInputSourceJson(), e);
        }
        if (initInput != null && inputLoadingStrategy != null) {
            register(initInput, request.getMandatory(), inputLoadingStrategy, request.getAmountOfRunsToUpdate());
        }

    }

    @Override
    public synchronized void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals) {
        NodeMeta meta = nodeMetas.get(name);
        if (meta == null) {
            logger.warn("Result upload from unknown node " + name + ", ignoring");
            return;
        }
        if (meta.getCurrentInput() == null) {
            logger.warn("Result upload from node " + name + " which has no assigned partition, ignoring");
            return;
        }
        // Shared storages (Redis) are written by the worker itself, in which case
        // the callback only carries the completion notification and no payload.
        if (signals != null && !signals.isEmpty()) {
            signalsPersist.putSignals(signals);
        }
        meta.setCurrentInput(null);
        meta.setStatus(true);
    }

    @Override
    public Boolean isProcessing(String name) {
        NodeMeta meta = nodeMetas.get(name);
        return meta != null && meta.getCurrentInput() != null;
    }

    @Override
    public void uploadDiscriminatorWorkerResult(String name, String nameDiscriminator, HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals) {
        discriminatorsSignalStorage.get(nameDiscriminator).putSignals(signals);
        nodeMetas.get(name).setStatus(true);
    }

    @Override
    public synchronized ISplitInput getNext(String name) {
        registerNode(name);
        ISplitInput res = assignPrepared(name);
        if (res == null) {
            prepareInputs();
            res = assignPrepared(name);
        }
        if (res == null) {
            res = reclaimTimedOutPartition(name);
        }
        return res;
    }

    /**
     * Registers a node that has not been seen before. A fresh node is idle: it holds no
     * partition, so it must not hold back the "all nodes idle" barrier in
     * {@link #prepareInputs()}. It joins at the layer the rest of the cluster is on.
     */
    private void registerNode(String name) {
        if (nodeMetas.containsKey(name)) {
            return;
        }
        Integer currentLayer = -1;
        if (!nodeMetas.isEmpty()) {
            currentLayer = nodeMetas.values().iterator().next().getCurrentLayer();
        }
        NodeMeta meta = new NodeMeta(currentLayer, true);
        meta.setTimestamp(System.currentTimeMillis());
        nodeMetas.put(name, meta);
    }

    private ISplitInput assignPrepared(String name) {
        if (preparedInputs.isEmpty()) {
            return null;
        }
        ISplitInput res = preparedInputs.remove(0);
        res.setNodeIdentifier(name);
        NodeMeta meta = nodeMetas.get(name);
        meta.setCurrentInput(res);
        meta.setStatus(false);
        meta.setTimestamp(System.currentTimeMillis());
        return res;
    }

    /**
     * Hands a partition owned by a node that stopped responding to the caller. The stale
     * node is dropped from the cluster so that it stops blocking the barrier.
     */
    private ISplitInput reclaimTimedOutPartition(String name) {
        String staleNode = null;
        ISplitInput orphaned = null;
        for (Map.Entry<String, NodeMeta> entry : nodeMetas.entrySet()) {
            if (entry.getKey().equals(name)) {
                continue;
            }
            NodeMeta meta = entry.getValue();
            if (meta.getCurrentInput() == null || meta.getTimestamp() == null) {
                continue;
            }
            if (System.currentTimeMillis() - meta.getTimestamp() > this.nodeTimeOut) {
                staleNode = entry.getKey();
                orphaned = meta.getCurrentInput();
                break;
            }
        }
        if (orphaned == null) {
            return null;
        }
        logger.warn("Node " + staleNode + " timed out, reassigning layer " + orphaned.getLayerId()
                + " partition [" + orphaned.getStart() + "," + orphaned.getEnd() + ") to " + name);
        nodeMetas.remove(staleNode);
        orphaned.setNodeIdentifier(name);
        NodeMeta meta = nodeMetas.get(name);
        meta.setCurrentInput(orphaned);
        meta.setStatus(false);
        meta.setTimestamp(System.currentTimeMillis());
        return orphaned;
    }

    /**
     * @return true when every known node is idle, i.e. the cluster finished the layer it
     * was working on and the next one may be prepared.
     */
    private boolean allNodesIdle() {
        for (NodeMeta meta : nodeMetas.values()) {
            if (!meta.getStatus()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized HashMap<String, NodeMeta> getNodeMetas() {
        return new HashMap<>(nodeMetas);
    }

    @Override
    public boolean hasNextComplete() {
        for (String n : nodeMetas.keySet()) {
            if (!nodeMetas.get(n).getStatus()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasPrepared() {
        return preparedInputs.size() > 0;
    }

    @Override
    public synchronized void prepareInputs() {
        if (!preparedInputs.isEmpty() || nodeMetas.isEmpty()) {
            return;
        }
        // Never advance to the next layer while somebody is still working on the current
        // one. Returning instead of blocking keeps the lock free for the nodes that still
        // have to report their results.
        if (!allNodesIdle()) {
            return;
        }
        if (layersMeta == null || splitInput == null || runningStrategy == null || signalsPersist == null) {
            logger.error("The input service is not fully configured; layers, split input, loading strategy"
                    + " and signal storage are all required before nodes can be scheduled");
            return;
        }
        int layerCount = layersMeta.getLayers().size();
        int currentPosition = nodeMetas.values().iterator().next().getCurrentLayer();
        int nextPosition = currentPosition + 1;

        if (nextPosition > layerCount) {
            boolean isDisc = false;
            for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
                if (!discriminatorStatus.isProcessed() || !discriminatorStatus.isValid()) {
                    break;
                }
                isDisc = true;
            }
            if (isDisc || discriminatorStatuses == null || discriminatorStatuses.size() == 0) {
                signalHistoryStorage.save(signalsPersist.getAllSignals(), runningStrategy.getEpoch(), runningStrategy.getCurrentLoopCount());
                // The input for the next epoch is populated by nextRun(), so that a completed
                // epoch feeds the network exactly once.
                runFlag = true;
            }
            return;
        }

        if (nextPosition == 0) {
            // Start of an epoch: feed the net before the first layer is handed out. Nothing else
            // populates the input, so without this the first epoch would process empty layers.
            runningStrategy.populateInput(signalsPersist, inputStatuses);
        }
        runFlag = false;
        boolean isResultLayer = nextPosition == layerCount;
        ILayerMeta layerMeta = isResultLayer ? layersMeta.getResultLayer() : layersMeta.getLayerByPosition(nextPosition);
        if (isResultLayer) {
            resultLayerHolder.setResultLayerMeta(layersMeta.getResultLayer());
        }
        preparedInputs.addAll(split(splitInput, layersMeta, layerMeta));
        for (NodeMeta meta : nodeMetas.values()) {
            meta.setCurrentLayer(nextPosition);
        }
    }

    /**
     * Cuts a layer into partitions of neuron index ranges. Every partition is a distinct
     * {@link ISplitInput} instance; the ranges are half open - {@code [start, end)} - and
     * together cover the whole layer exactly once.
     */
    private List<ISplitInput> split(ISplitInput prototype, ILayersMeta meta, ILayerMeta layerMeta) {
        List<ISplitInput> resList = new ArrayList<>();
        long layerSize = layerMeta.getSize() == null ? 0l : layerMeta.getSize();
        if (layerSize <= 0) {
            return resList;
        }
        int nodeCount = Math.max(nodeMetas.size(), 1);
        long partitionCount = Math.max(partitions == null ? 1 : partitions, nodeCount);
        partitionCount = Math.min(partitionCount, layerSize);
        long atomic = Math.max(layerSize / partitionCount, 1);
        for (long i = 0; i < partitionCount; i++) {
            long start = i * atomic;
            long end = i == partitionCount - 1 ? layerSize : Math.min((i + 1) * atomic, layerSize);
            if (start >= end) {
                break;
            }
            ISplitInput input = prototype.getNewInstance();
            input.applyMeta(meta);
            input.applyRunState(runningStrategy.getEpoch(), runningStrategy.getCurrentLoopCount(), runningStrategy.getNeuronInputMapping());
            input.setLayer(layerMeta.getID());
            input.setStart(start);
            input.setEnd(end);
            resList.add(input);
        }
        return resList;
    }

    @Override
    public Boolean runCompleted() {
        if (nodeMetas.isEmpty()) {
            return false;
        }
        // The result layer sits one position past the last regular layer. Comparing layer
        // *positions* here - comparing layer ids does not work because implementations are
        // free to give the result layer any id (the Redis one uses Integer.MAX_VALUE).
        if (nodeMetas.values().iterator().next().getCurrentLayer() < layersMeta.getLayers().size()) {
            return false;
        }
        return allNodesIdle();
    }

    @Override
    public void prepareResults() {
        if (this.runCompleted()) {
            runFlag = true;
            List<IResultNeuron> neurons = (List<IResultNeuron>) this.resultLayerRunner.getResults(layersMeta.getResultLayer(), signalsPersist.getLayerSignals(layersMeta.getResultLayer().getID()));
            if (results.containsKey(runningStrategy.getEpoch())) {
                results.get(runningStrategy.getEpoch()).put(runningStrategy.getCurrentLoopCount(), neurons);
            } else {
                HashMap<Integer, List<IResultNeuron>> resultNeurons = new HashMap<>();
                resultNeurons.put(runningStrategy.getCurrentLoopCount(), neurons);
                results.put(runningStrategy.getEpoch(), resultNeurons);
            }
        }
    }

    @Override
    public synchronized void nextRun() {
        if (runFlag) {
            runFlag = false;
            if (runOnceIn != null && runOnceIn > 0) {
                try {
                    Thread.sleep(runOnceIn);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    logger.error("Interrupted while pacing the next run", e);
                }
            }
            run++;
            // Back to "before the first layer"; prepareInputs populates the input for the new
            // epoch when it prepares position 0.
            for (NodeMeta meta : nodeMetas.values()) {
                meta.setCurrentLayer(-1);
            }
        }

    }

    @Override
    public void nextRunDiscriminator() {
        for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
            discriminatorStatus.setProcessed(false);
            discriminatorStatus.setValid(false);
            discriminatorStatus.setCurrentLayer(-1);
            discriminatorStatus.setInputPopulated(false);
        }
        for (NodeMeta meta : nodeMetas.values()) {
            meta.setCurrentLayer(-1);
        }
    }


    @Override
    public void setLayersMeta(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    @Override
    public void setRun(Long run) {
        this.run = run;
    }

    @Override
    public void processCallBackFromUpstream(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals) {
        signalsPersist.putSignals(signals);
    }

    @Override
    public ILayersMeta getLayersMeta() {
        return layersMeta;
    }

    @Override
    public void updateLayersMeta(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    @Override
    public void updateDiscriminators(HashMap<String, ILayersMeta> discriminators) {
        this.discriminators = discriminators;
        this.discriminatorStatuses.clear();
        for (String name : discriminators.keySet()) {
            discriminatorStatuses.add(new DiscriminatorStatus(name, false, false, 0, false));
        }

    }

    @Override
    public boolean hasDiscriminators() {
        return discriminators.size() > 0;
    }

    @Override
    public synchronized void prepareDiscriminatorsInputs() {
        DiscriminatorStatus currentDiscriminator = null;
        for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
            if (!discriminatorStatus.isProcessed()) {
                currentDiscriminator = discriminatorStatus;

            }
        }
        boolean isDisc = false;

        if (preparedDiscriminatorsInputs.size() == 0 && currentDiscriminator != null) {
            String discriminatorName = currentDiscriminator.getName();
            if (!currentDiscriminator.isInputPopulated()) {
                discriminatorsLoadingStrategies.get(discriminatorName).populateInput(discriminatorsSignalStorage.get(discriminatorName), inputDiscriminatorStatuses.get(discriminatorName));
            }
            if (nodeMetas.isEmpty() || !allNodesIdle()) {
                return;
            }
            ILayersMeta discriminatorLayersMeta = discriminators.get(discriminatorName);
            int nextPosition = nodeMetas.values().iterator().next().getCurrentLayer() + 1;
            if (nextPosition <= discriminatorLayersMeta.getLayers().size()) {
                boolean isResultLayer = nextPosition == discriminatorLayersMeta.getLayers().size();
                ILayerMeta layerMeta = isResultLayer ? discriminatorLayersMeta.getResultLayer() : discriminatorLayersMeta.getLayerByPosition(nextPosition);
                preparedDiscriminatorsInputs.addAll(split(discriminatorSplitInput, discriminatorLayersMeta, layerMeta));
                for (NodeMeta meta : nodeMetas.values()) {
                    meta.setCurrentLayer(nextPosition);
                }
                currentDiscriminator.setCurrentLayer(nextPosition);
            } else {

                DiscriminatorResultLayer resultLayer = (DiscriminatorResultLayer) discriminatorLayersMeta.getResultLayer();
                currentDiscriminator.setValid(resultLayer.hasPass());
                ISignalHistoryStorage discriminatorSignalHistoryStorage = discriminatorsSignalStorageHistory.get(discriminatorName);
                ISignalsPersistStorage discriminatorSignalsPersistStorage = discriminatorsSignalStorage.get(discriminatorName);
                IInputLoadingStrategy discriminatorInputLoadingStrategy = discriminatorsLoadingStrategies.get(discriminatorName);
                currentDiscriminator.setProcessed(true);
                discriminatorSignalHistoryStorage.save(discriminatorSignalsPersistStorage.getAllSignals(), discriminatorInputLoadingStrategy.getEpoch(), discriminatorInputLoadingStrategy.getCurrentLoopCount());
                discriminatorSignalsPersistStorage.cleanOutdatedSignals();
                discriminatorInputLoadingStrategy.populateInput(discriminatorSignalsPersistStorage, inputDiscriminatorStatuses.get(discriminatorName));

                for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
                    if (!discriminatorStatus.isProcessed() || !discriminatorStatus.isValid()) {
                        break;
                    }
                    isDisc = true;
                }
                if (isDisc) {
                    signalHistoryStorage.save(signalsPersist.getAllSignals(), runningStrategy.getEpoch(), runningStrategy.getCurrentLoopCount());
                    signalsPersist.cleanOutdatedSignals();
                    runningStrategy.populateInput(signalsPersist, inputStatuses);
                    runFlag = true;
                }
            }
        } else {
            for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
                if (!discriminatorStatus.isProcessed() || !discriminatorStatus.isValid()) {
                    break;
                }
                isDisc = true;
            }
            if (isDisc) {
                signalHistoryStorage.save(signalsPersist.getAllSignals(), runningStrategy.getEpoch(), runningStrategy.getCurrentLoopCount());
                signalsPersist.cleanOutdatedSignals();
                runningStrategy.populateInput(signalsPersist, inputStatuses);
                runFlag = true;
            }
        }

    }

    @Override
    public boolean isDiscriminatorsDone() {
        for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
            if (!discriminatorStatus.isProcessed()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isResultValid() {
        for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
            if (!discriminatorStatus.isValid()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized ISplitInput getNextDiscriminators(String name) {
        registerNode(name);
        ISplitInput res = assignPreparedDiscriminator(name);
        if (res == null) {
            prepareDiscriminatorsInputs();
            res = assignPreparedDiscriminator(name);
        }
        if (res == null) {
            res = reclaimTimedOutPartition(name);
        }
        return res;
    }

    private ISplitInput assignPreparedDiscriminator(String name) {
        if (preparedDiscriminatorsInputs.isEmpty()) {
            return null;
        }
        ISplitInput res = preparedDiscriminatorsInputs.remove(0);
        res.setNodeIdentifier(name);
        NodeMeta meta = nodeMetas.get(name);
        meta.setCurrentInput(res);
        meta.setStatus(false);
        meta.setTimestamp(System.currentTimeMillis());
        return res;
    }

}
