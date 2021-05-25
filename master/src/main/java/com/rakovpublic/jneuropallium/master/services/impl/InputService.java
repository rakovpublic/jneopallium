package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.exceptions.InputServiceInitException;
import com.rakovpublic.jneuropallium.master.services.IInputLoadingStrategy;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.neuron.IResultNeuron;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.SortedSet;

public class InputService implements IInputService {
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<String, NodeMeta> nodeMetas;
    private HashMap<IInitInput, InputInitStrategy> inputs;
    private ISignalsPersistStorage signalsPersist;
    private ILayersMeta layersMeta;
    private List<ISplitInput> preparedInputs;
    private ISplitInput splitInput;
    private Integer partitions;
    private IInputLoadingStrategy runningStrategy;


    public InputService(ISignalsPersistStorage signalsPersist, ILayersMeta layersMeta, ISplitInput splitInput, Integer partitions, IInputLoadingStrategy runningStrategy) {
        this.signalsPersist = signalsPersist;
        this.layersMeta = layersMeta;
        this.preparedInputs = new ArrayList<>();
        this.splitInput = splitInput;
        this.partitions = partitions;
        this.runningStrategy = runningStrategy;
        this.nodeMetas = new HashMap<>();
        this.inputs = new HashMap<>();
        this.inputStatuses = new HashMap<>();
    }

    @Override
    public void inputSourceUpdated(String name) {
        inputStatuses.get(name).setStatus(true);
    }

    @Override
    public void register( IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {
        //signalsPersist.putSignals(initStrategy.getInputs(layersMeta, iInputSource.readSignals()));
        inputStatuses.put(iInputSource, new InputStatusMeta(true, isMandatory,amountOfRuns));
        inputs.put(iInputSource,initStrategy);
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
            res.setNodeIdentifier(name);
            nodeMetas.get(name).setStatus(false);
        } else {
            prepareInputs();
            if (preparedInputs.size() > 0) {
                res = preparedInputs.get(0);
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
                        //TODO: add logger
                        e.printStackTrace();
                    }
                }
            }
            if (nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1 >= layersMeta.getLayers().size()) {
                ILayerMeta layerMeta = layersMeta.getLayerByID(nodeMetas.get(nodeNames.get(0)).getCurrentLayer() + 1);
                Long size = layerMeta.getSize() / nodeNames.size() <= partitions ? new Long(partitions) : nodeNames.size();
                List<ISplitInput> resList = new ArrayList<>();
                ISplitInput input = splitInput.getNewInstance();
                input.setNodeIdentifier(nodeNames.get(0));
                HashMap<Long, List<ISignal>> layerInput = signalsPersist.getLayerSignals(layerMeta.getID());
                HashMap<Long, List<ISignal>> res = new HashMap<>();
                for (Long l = 0l; l <= size; l++) {
                    if (layerInput.containsKey(l)) {
                        res.put(l, layerInput.get(l));
                        layerInput.remove(l);
                    }
                    if (l == size) {
                        input.saveResults(res);
                        resList.add(input);
                        if (layerInput.keySet().size() > 0) {
                            res = new HashMap<>();
                            input = splitInput.getNewInstance();
                            size += size;
                        }

                    }
                }
                for (String nodeName : nodeNames) {
                    nodeMetas.get(nodeName).setCurrentLayer(layerMeta.getID());
                }
            }
        }

    }

    @Override
    public Boolean runCompleted() {
        return null;
    }

    @Override
    public SortedSet<? extends IResultNeuron> prepareResults() {
        return null;
    }

    @Override
    public void nextRun() {

    }

    @Override
    public void setLayersMeta(ILayersMeta layersMeta) {

    }
}
