package com.rakovpublic.jneuropallium.master.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.DiscriminatorResultLayer;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.ResultLayerHolder;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

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
    private HashMap<Long, HashMap<Integer, List<IResultNeuron>>> results;
    private Long nodeTimeOut;
    private ResultLayerHolder resultLayerHolder;


    public InputService(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner, HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies, HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage, HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory, HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses, ISplitInput discriminatorSplitInput, Long nodeTimeOut, ResultLayerHolder resultLayerHolder) {
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
    }


    @Override
    public List<IResultNeuron> getResults(Integer loop, Long epoch) {
        return results.get(epoch).get(loop);
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
    }

    @Override
    public void inputSourceUpdated(String name) {
        inputStatuses.get(name).setStatus(true);
    }

    @Override
    public void register(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {
        inputStatuses.put(iInputSource, new InputStatusMeta(true, isMandatory, iInputSource.getName()));
        inputs.put(iInputSource, initStrategy);
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
    public void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        if (nodeMetas.get(name).getCurrentInput() != null) {
            signalsPersist.putSignals(signals);
            nodeMetas.get(name).setStatus(true);
        }
    }

    @Override
    public Boolean isProcessing(String name) {
        return nodeMetas.get(name).getCurrentInput() != null;
    }

    @Override
    public void uploadDiscriminatorWorkerResult(String name, String nameDiscriminator, HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        discriminatorsSignalStorage.get(nameDiscriminator).putSignals(signals);
        nodeMetas.get(name).setStatus(true);
    }

    @Override
    public synchronized ISplitInput getNext(String name) {
        ISplitInput res = null;
        if (preparedInputs.size() > 0) {
            res = preparedInputs.get(0);
            preparedInputs.remove(0);
            res.setNodeIdentifier(name);
            nodeMetas.get(name).setStatus(false);
            nodeMetas.get(name).setTimestamp(System.currentTimeMillis());
        } else {
            prepareInputs();
            if (preparedInputs.size() > 0) {
                res = preparedInputs.get(0);
                preparedInputs.remove(0);
                res.setNodeIdentifier(name);
                nodeMetas.get(name).setStatus(false);
                nodeMetas.get(name).setTimestamp(System.currentTimeMillis());
            } else {
                for (NodeMeta meta : nodeMetas.values()) {
                    if (System.currentTimeMillis() - meta.getTimestamp() > this.nodeTimeOut) {
                        res = meta.getCurrentInput();
                        res.setNodeIdentifier(name);
                        meta.setCurrentInput(null);
                        meta.setStatus(true);
                        nodeMetas.get(name).setStatus(false);
                        nodeMetas.get(name).setTimestamp(System.currentTimeMillis());
                    }
                }
            }
        }
        return res;
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

        if (preparedInputs.size() == 0) {
            List<String> nodeNames = new ArrayList<>();
            nodeNames.addAll(nodeMetas.keySet());
            for (int i = 0; i < nodeNames.size(); i++) {
                if (!nodeMetas.get(nodeNames.get(i)).getStatus()) {
                    i = 0;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }
            }
            if (nodeMetas.get(nodeNames.get(0)).getCurrentLayer() <= layersMeta.getResultLayer().getID()) {
                if (nodeMetas.get(nodeNames.get(0)).getCurrentLayer() == layersMeta.getResultLayer().getID()) {
                    resultLayerHolder.setResultLayerMeta(layersMeta.getResultLayer());
                }
                runFlag = false;
                ILayerMeta layerMeta = nodeMetas.get(nodeNames.get(0)).getCurrentLayer() == layersMeta.getResultLayer().getID() ? layersMeta.getResultLayer() : layersMeta.getLayerByID(nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1);
                Long size = layerMeta.getSize() / nodeNames.size() <= partitions ? Long.parseLong(partitions + "") : nodeNames.size();
                List<ISplitInput> resList = new ArrayList<>();
                ISplitInput input = splitInput.getNewInstance();
                input.applyMeta(layersMeta);
                Long atomic = layerMeta.getSize() / size > 1 ? layerMeta.getSize() / size : 1;
                for (int i = 0; i < size; i++) {
                    input.setStart(i * atomic);
                    input.setEnd((i + 1) * atomic < layerMeta.getSize() ? (i + 1) * atomic : layerMeta.getSize() - 1);
                    input.setNodeIdentifier(nodeNames.get(0));
                    input.setLayer(layerMeta.getID());
                    resList.add(input);

                }
                for (String nodeName : nodeNames) {
                    nodeMetas.get(nodeName).setCurrentLayer(layerMeta.getID());
                }
                preparedInputs.addAll(resList);
            } else {
                boolean isDisc = false;
                for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
                    if (!discriminatorStatus.isProcessed() || !discriminatorStatus.isValid()) {
                        break;
                    }
                    isDisc = true;
                }
                if (isDisc || discriminatorStatuses == null || discriminatorStatuses.size() == 0) {
                    signalHistoryStorage.save(signalsPersist.getAllSignals(), runningStrategy.getEpoch(), runningStrategy.getCurrentLoopCount());
                    signalsPersist.cleanOutdatedSignals();
                    runningStrategy.populateInput(signalsPersist, inputStatuses);
                    runFlag = true;
                }
            }
        }

    }

    @Override
    public Boolean runCompleted() {
        if (layersMeta.getResultLayer().getID() == nodeMetas.values().iterator().next().getCurrentLayer()) {
            for (NodeMeta meta : nodeMetas.values()) {
                if (!meta.getStatus()) {
                    return false;
                }
            }
            return true;
        }
        return false;
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
    public void nextRun() {
        if (runFlag) {
            runFlag = false;
            runningStrategy.populateInput(signalsPersist, inputStatuses);
            run++;
        }
    }

    @Override
    public void nextRunDiscriminator() {
        for (DiscriminatorStatus discriminatorStatus : discriminatorStatuses) {
            discriminatorStatus.setProcessed(false);
            discriminatorStatus.setValid(false);
            discriminatorStatus.setCurrentLayer(0);
            discriminatorStatus.setInputPopulated(false);
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
    public void processCallBackFromUpstream(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
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
    public void prepareDiscriminatorsInputs() {
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
            List<String> nodeNames = new ArrayList<>();
            nodeNames.addAll(nodeMetas.keySet());
            for (int i = 0; i < nodeNames.size(); i++) {
                if (!nodeMetas.get(nodeNames.get(i)).getStatus()) {
                    i = 0;
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }
            }
            ILayersMeta discriminatorLayersMeta = discriminators.get(discriminatorName);
            if (currentDiscriminator.getCurrentLayer() + 1 <= discriminatorLayersMeta.getResultLayer().getID()) {

                ILayerMeta layerMeta = currentDiscriminator.getCurrentLayer() + 1 == discriminatorLayersMeta.getResultLayer().getID() ? discriminatorLayersMeta.getResultLayer() : discriminatorLayersMeta.getLayerByID(nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1);
                Long size = layerMeta.getSize() / nodeNames.size() <= partitions ? Long.parseLong(partitions + "") : nodeNames.size();
                List<ISplitInput> resList = new ArrayList<>();
                ISplitInput input = discriminatorSplitInput.getNewInstance();
                input.applyMeta(discriminators.get(currentDiscriminator.getName()));
                Long atomic = layerMeta.getSize() / size > 1 ? layerMeta.getSize() / size : 1;
                for (int i = 0; i < size; i++) {
                    input.setStart(i * atomic);
                    input.setEnd((i + 1) * atomic);
                    input.setNodeIdentifier(nodeNames.get(0));
                    input.setLayer(layerMeta.getID());
                    resList.add(input);

                }
                for (String nodeName : nodeNames) {
                    nodeMetas.get(nodeName).setCurrentLayer(layerMeta.getID());
                }
                currentDiscriminator.setCurrentLayer(nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1);
                preparedDiscriminatorsInputs.addAll(resList);
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
    public ISplitInput getNextDiscriminators(String name) {
        ISplitInput res = null;
        if (preparedDiscriminatorsInputs.size() > 0) {
            res = preparedDiscriminatorsInputs.get(0);
            preparedDiscriminatorsInputs.remove(0);
            res.setNodeIdentifier(name);
            nodeMetas.get(name).setStatus(false);
            nodeMetas.get(name).setTimestamp(System.currentTimeMillis());
        } else {
            prepareInputs();
            if (preparedDiscriminatorsInputs.size() > 0) {
                res = preparedDiscriminatorsInputs.get(0);
                preparedDiscriminatorsInputs.remove(0);
                res.setNodeIdentifier(name);
                nodeMetas.get(name).setStatus(false);
                nodeMetas.get(name).setTimestamp(System.currentTimeMillis());
            } else {
                for (NodeMeta meta : nodeMetas.values()) {
                    if (System.currentTimeMillis() - meta.getTimestamp() > this.nodeTimeOut) {
                        res = meta.getCurrentInput();
                        meta.setCurrentInput(null);
                        res.setNodeIdentifier(name);
                        meta.setStatus(true);
                        nodeMetas.get(name).setStatus(false);
                        nodeMetas.get(name).setTimestamp(System.currentTimeMillis());
                    }
                }
            }
        }
        return res;
    }

}
