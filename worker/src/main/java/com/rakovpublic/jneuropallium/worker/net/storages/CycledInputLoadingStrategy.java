package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.InMemoryLayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.CycleNeuron;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.CycleSignalsProcessingChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.ProcessingFrequency;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;


/**
 * Input loading strategy designed to have different input loading delay for each init input
 */
public class CycledInputLoadingStrategy implements IInputLoadingStrategy {
    private ILayersMeta layersMeta;
    private HashMap<IInitInput, InputInitStrategy> externalInputs;
    private Integer loop;
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<String, Long> neuronInputMapping;
    private Long epoch;
    int defaultLoopsCount;
    private  HashMap<Long, HashMap<Integer, List<ISignal>>> inputHistory;


    public CycledInputLoadingStrategy(ILayersMeta layersMeta, HashMap<IInitInput, InputInitStrategy> externalInputs, int defaultLoopsCount, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        epoch = 0L;
        loop = 0;
        this.layersMeta = layersMeta;
        this.externalInputs = externalInputs;
        this.inputStatuses = inputStatuses;
        this.defaultLoopsCount = defaultLoopsCount;
        init(defaultLoopsCount);
        neuronInputMapping = new HashMap<>();
        inputHistory = new HashMap<>();

    }

    private void init(int defaultLoopsCount) {
        ISignalChain signalChain = new CycleSignalsProcessingChain();
        CycleNeuron cycleNeuron = new CycleNeuron(defaultLoopsCount, signalChain, null, 0l, epoch);
        List<INeuron> neurons = new LinkedList<>();
        neurons.add(cycleNeuron);
        long neuronId = 1l;
        for (InputStatusMeta meta : inputStatuses.values()) {
            neurons.add(new CycleNeuron(defaultLoopsCount, signalChain, meta, neuronId, epoch));
            neuronInputMapping.put(meta.getName(), neuronId);
            neuronId += 1;
        }
        ILayerMeta layerMeta = new InMemoryLayerMeta(Integer.MIN_VALUE, neurons);
        layersMeta.addLayerMeta(layerMeta);

    }

    @Override
    public Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        signalsPersistStorage.cleanOutdatedSignals();
        CycleNeuron cl = ((CycleNeuron) layersMeta.getLayerByID(Integer.MIN_VALUE).getNeuronByID(0l));
        HashMap<Class<? extends ISignal>, ProcessingFrequency> frequencyHashMap = cl.getSignalProcessingFrequencyMap();
        CycleNeuron cycleNeuron = (CycleNeuron) layersMeta.getLayerByID(Integer.MIN_VALUE).getNeuronByID(0l);
        HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequencyHashMap = cycleNeuron.getInputProcessingFrequencyHashMap();
        if (loop >= cl.getLoopCount()) {
            for (IInitInput iii : inputStatuses.keySet()) {
                ProcessingFrequency ipf = null;
                if (inputProcessingFrequencyHashMap.containsKey(iii)) {
                    ipf = inputProcessingFrequencyHashMap.get(iii);
                }
                if (inputStatuses.get(iii).getCurrentRuns() %
                        cycleNeuron.getLoopCount() == 0 && (ipf != null && (ipf.getLoop() != null && loop % ipf.getLoop() == 0) || (ipf.getEpoch() != null && epoch % ipf.getEpoch() == 0))) {
                    List<ISignal> signals = new LinkedList<>();
                    List<ISignal> signalsHistory = new LinkedList<>();
                    for (IInputSignal signal : iii.readSignals()) {
                        ProcessingFrequency pf = frequencyHashMap.get(signal.getCurrentSignalClass());
                        if (loop % pf.getLoop() == 0 && epoch % pf.getEpoch() == 0) {
                            signal.setInnerLoop(defaultLoopsCount);
                            signal.setEpoch(epoch);
                            signal.setLoop(loop);
                            signalsHistory.add(signal.copySignal());
                            signals.add(signal);
                        }
                    }
                    signalsPersistStorage.putSignals(externalInputs.get(iii).getInputs(layersMeta, signals));
                    inputStatuses.get(iii).setCurrentRuns(0);
                    inputStatuses.get(iii).setBeenUsed(true);
                    if(inputHistory.containsKey(epoch)){
                        if(inputHistory.get(epoch).containsKey(loop)){
                            inputHistory.get(epoch).get(loop).addAll(signalsHistory);
                        }else {
                            HashMap<Integer,List<ISignal>> history=  new HashMap<>();
                            history.put(loop,signalsHistory);
                            inputHistory.put(epoch,history);
                        }
                    }else {
                        HashMap<Integer,List<ISignal>> history=  new HashMap<>();
                        history.put(loop,signalsHistory);
                        inputHistory.put(epoch,history);
                    }
                } else {
                    inputStatuses.get(iii).setCurrentRuns(inputStatuses.get(iii).getCurrentRuns() + 1);
                }
            }
            loop = 0;
            if (epoch == Long.MAX_VALUE) {
                epoch = Long.MIN_VALUE + 2;
            } else {
                epoch += 1;
            }

        } else {
            loop += 1;
        }
        return true;
    }


    @Override
    public void setLayersMeta(ILayersMeta layersMeta) {
        this.layersMeta = layersMeta;
    }

    @Override
    public HashMap<String, Long> getNeuronInputMapping() {
        return neuronInputMapping;
    }

    @Override
    public Integer getCurrentLoopCount() {
        return ((CycleNeuron) layersMeta.getLayerByID(Integer.MIN_VALUE).getNeuronByID(0L)).getLoopCount();
    }

    @Override
    public Long getEpoch() {
        return epoch;
    }

    @Override
    public void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs) {
        ISignalChain signalChain = new CycleSignalsProcessingChain();
        TreeSet<Long> ids = new TreeSet<>();
        ids.addAll(neuronInputMapping.values());
        long neuronId = ids.last() + 1;
        ILayerMeta layerMeta = layersMeta.getLayerByID(Integer.MIN_VALUE);
        List<INeuron> neurons = new LinkedList<>();
        if(layerMeta == null){
            layerMeta = new InMemoryLayerMeta(Integer.MIN_VALUE, neurons);
            layersMeta.addLayerMeta(layerMeta);
        }
        neurons.addAll(layerMeta.getNeurons());
        for (InputStatusMeta meta : inputStatuses.values()) {
            if (!neuronInputMapping.containsKey(meta.getName())) {
                neurons.add(new CycleNeuron(defaultLoopsCount, signalChain, meta, neuronId, epoch));
                neuronInputMapping.put(meta.getName(), neuronId);
                neuronId += 1;
            }
        }
    }

    @Override
    public HashMap<Long, HashMap<Integer, List<ISignal>>> getInputHistory() {
        return inputHistory;
    }
}
