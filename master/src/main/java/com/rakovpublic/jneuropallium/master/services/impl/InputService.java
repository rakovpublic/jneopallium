package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.model.InputRegistrationRequest;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
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

    public InputService(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner) {
        this.signalsPersist = signalsPersist;
        this.layersMeta = layersMeta;
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
    }

    @Override
    public void updateConfiguration(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy, ISignalHistoryStorage signalHistoryStorage, IResultLayerRunner resultLayerRunner) {
        this.signalsPersist = signalsPersist;
        this.layersMeta = layersMeta;
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
    }

    @Override
    public void inputSourceUpdated(String name) {
        inputStatuses.get(name).setStatus(true);
    }

    @Override
    public void register(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {
        //signalsPersist.putSignals(initStrategy.getInputs(layersMeta, iInputSource.readSignals()));
        inputStatuses.put(iInputSource, new InputStatusMeta(true, isMandatory, amountOfRuns, iInputSource.getName()));
        inputs.put(iInputSource, initStrategy);
    }

    @Override
    public void register(InputRegistrationRequest request) {
//TODO: implement
    }

    @Override
    public void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        signalsPersist.putSignals(signals);
        nodeMetas.get(name).setStatus(true);
    }

    @Override
    public synchronized ISplitInput getNext(String name) {
        ISplitInput res;
        if (preparedInputs.size() > 0) {
            res = preparedInputs.get(0);
            preparedInputs.remove(0);
            res.setNodeIdentifier(name);
            nodeMetas.get(name).setStatus(false);
        } else {
            prepareInputs();
            if (preparedInputs.size() > 0) {
                res = preparedInputs.get(0);
                preparedInputs.remove(0);
                res.setNodeIdentifier(name);
                nodeMetas.get(name).setStatus(false);
            } else {
                res = null;
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
            if (nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1 < layersMeta.getLayers().size()) {
                runFlag = false;
                ILayerMeta layerMeta = layersMeta.getLayerByID(nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1);
                Long size = layerMeta.getSize() / nodeNames.size() <= partitions ? Long.parseLong(partitions + "") : nodeNames.size();
                List<ISplitInput> resList = new ArrayList<>();
                ISplitInput input = splitInput.getNewInstance();
                Long atomic = layerMeta.getSize() / size > 1 ? layerMeta.getSize() / size : 1;
                for (int i = 0; i < size; i++) {
                    input.setStart(i * atomic);
                    input.setEnd((i + 1) * atomic);
                    input.setNodeIdentifier(nodeNames.get(0));
                    input.setLayer(layerMeta.getID());
                    input.setCurrentLoopCount(runningStrategy.getCurrentLoopCount());
                    input.setRun(runningStrategy.getEpoch());
                    resList.add(input);

                }
                for (String nodeName : nodeNames) {
                    nodeMetas.get(nodeName).setCurrentLayer(layerMeta.getID());
                }
            } else {
                signalHistoryStorage.save(signalsPersist.getAllSignals(), runningStrategy.getEpoch(), runningStrategy.getCurrentLoopCount());
                signalsPersist.cleanOutdatedSignals();
                runningStrategy.populateInput(signalsPersist, inputStatuses);
                runFlag = true;
            }
        }

    }

    @Override
    public Boolean runCompleted() {
        if (layersMeta.getLayers().size() == nodeMetas.values().iterator().next().getCurrentLayer()) {
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
    public List<? extends IResultNeuron> prepareResults() {
        if (this.runCompleted()) {
            runFlag = true;
            return this.resultLayerRunner.getResults(layersMeta.getResultLayer(), signalsPersist.getLayerSignals(layersMeta.getResultLayer().getID()));
        }
        return null;

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
}
