package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.exceptions.InputServiceInitException;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.IInputLoadingStrategy;
import com.rakovpublic.jneuropallium.master.services.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SingleNetInputService implements IInputService {
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<String, NodeMeta> nodeMetas;
    private HashMap<IInitInput,InputInitStrategy> inputs;
    private ISignalsPersistStorage signalsPersist;
    private ILayersMeta layersMeta;
    private static SingleNetInputService singleNetInputService = new SingleNetInputService();
    private List<ISplitInput> preparedInputs;
    private ISplitInput splitInput;
    private Integer partitions;
    private IInputLoadingStrategy runningStrategy;

    private SingleNetInputService() {
        inputStatuses = new HashMap<>();
        inputs= new HashMap<>();
        nodeMetas = new HashMap<>();
        preparedInputs = new ArrayList<>();
    }

    public static SingleNetInputService getInputService(ILayersMeta meta, ISignalsPersistStorage storage, ISplitInput splitInputSample, Integer partitions, IInputLoadingStrategy runningStrategy) throws InputServiceInitException {
        if (singleNetInputService.layersMeta == null || meta != null)
            singleNetInputService.layersMeta = meta;
        if (singleNetInputService.signalsPersist == null || storage != null)
            singleNetInputService.signalsPersist = storage;
        if (singleNetInputService.splitInput == null || splitInputSample != null)
            singleNetInputService.splitInput = splitInputSample;
        if(runningStrategy!=null || singleNetInputService.runningStrategy==null)
            singleNetInputService.runningStrategy=runningStrategy;
        if (singleNetInputService.signalsPersist == null || singleNetInputService.layersMeta == null || singleNetInputService.splitInput == null || singleNetInputService.runningStrategy == null) {
            //TODO:add logger
            throw new InputServiceInitException();
        }
        if (partitions == null) {
            singleNetInputService.partitions = 7;
        } else {
            singleNetInputService.partitions = partitions;
        }
        return singleNetInputService;
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
}
