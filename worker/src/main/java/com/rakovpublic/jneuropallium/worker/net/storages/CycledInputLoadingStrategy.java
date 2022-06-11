package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.InMemoryLayerMeta;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.CycleNeuron;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.CycleSignalsProcessingChain;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;


/**
 * Input loading strategy designed to have different input loading delay for each init input
 *
 * */
public class CycledInputLoadingStrategy implements IInputLoadingStrategy {
    private ILayersMeta layersMeta;
    private HashMap<IInitInput, InputInitStrategy> externalInputs;
    private Integer counter;
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<String, Long> neuronInputMapping;
    private Long run;
    int defaultLoopsCount;

    public CycledInputLoadingStrategy(ILayersMeta layersMeta, HashMap<IInitInput, InputInitStrategy> externalInputs, int defaultLoopsCount, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        run=0l;
        counter = 0;
        this.layersMeta = layersMeta;
        this.externalInputs = externalInputs;
        this.inputStatuses = inputStatuses;
        this.defaultLoopsCount = defaultLoopsCount;
        init(defaultLoopsCount);
        neuronInputMapping = new HashMap<>();

    }

    private void init(int defaultLoopsCount) {
        ISignalChain signalChain = new CycleSignalsProcessingChain();
        CycleNeuron cycleNeuron = new CycleNeuron(defaultLoopsCount, signalChain, null, 0l, run);
        List<INeuron> neurons = new LinkedList<>();
        neurons.add(cycleNeuron);
        long neuronId = 1l;
        for (InputStatusMeta meta : inputStatuses.values()) {
            neurons.add(new CycleNeuron(defaultLoopsCount, signalChain, meta, neuronId, run));
            neuronInputMapping.put(meta.getName(), neuronId);
            neuronId += 1;
        }
        ILayerMeta layerMeta = new InMemoryLayerMeta(Integer.MIN_VALUE, neurons);
        layersMeta.addLayerMeta(layerMeta);

    }

    @Override
    public Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        signalsPersistStorage.cleanOutdatedSignals();
        if (counter >= ((CycleNeuron) layersMeta.getLayerByID(Integer.MIN_VALUE).getNeuronByID(0l)).getLoopCount()) {
            for (IInitInput iii : inputStatuses.keySet()) {
                if (inputStatuses.get(iii).getCurrentRuns() >=
                        ((CycleNeuron) layersMeta.getLayerByID(Integer.MIN_VALUE).getNeuronByID(neuronInputMapping.get(inputStatuses.get(iii).getName()))).getLoopCount()) {
                    List<ISignal> signals = new LinkedList<>();
                    for (IInputSignal signal : iii.readSignals()){
                        signal.setInnerLoop(defaultLoopsCount);
                        signals.add(signal);
                    }
                    signalsPersistStorage.putSignals(externalInputs.get(iii).getInputs(layersMeta,signals));
                    inputStatuses.get(iii).setCurrentRuns(0);
                } else {
                    inputStatuses.get(iii).setCurrentRuns(inputStatuses.get(iii).getCurrentRuns() + 1);
                }
            }
            counter = 0;
            run+=1;
        } else {
            counter += 1;
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
        return ((CycleNeuron) layersMeta.getLayerByID(Integer.MIN_VALUE).getNeuronByID(0l)).getLoopCount();
    }

    @Override
    public Long getEpoch() {
        return run;
    }

    @Override
    public void updateInputs(HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<IInitInput, InputInitStrategy> inputs) {
        ISignalChain signalChain = new CycleSignalsProcessingChain();
        TreeSet<Long> ids = new TreeSet<>();
        ids.addAll(neuronInputMapping.values());
        long neuronId = ids.last() + 1;
        ILayerMeta layerMeta = layersMeta.getLayerByID(Integer.MIN_VALUE);
        List<INeuron> neurons = new LinkedList<>();
        neurons.addAll(layerMeta.getNeurons());
        for (InputStatusMeta meta : inputStatuses.values()) {
            if (!neuronInputMapping.containsKey(meta.getName())) {
                neurons.add(new CycleNeuron(defaultLoopsCount, signalChain, meta, neuronId, run));
                neuronInputMapping.put(meta.getName(), neuronId);
                neuronId += 1;
            }
        }
    }
}
