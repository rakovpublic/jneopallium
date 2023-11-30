/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.exceptions.ConfigurationClassMissedException;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InMemoryLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.CycleNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.CycleSignalsProcessingChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;


/**
 * Input loading strategy designed to have different input loading delay for each init input
 */
public class CycledInputLoadingStrategy implements IInputLoadingStrategy {
    private static final Logger logger = LogManager.getLogger(CycledInputLoadingStrategy.class);
    private ILayersMeta layersMeta;
    private HashMap<IInitInput, InputInitStrategy> externalInputs;
    private Integer loop;
    private HashMap<IInitInput, InputStatusMeta> inputStatuses;
    private HashMap<String, Long> neuronInputMapping;
    private Long epoch;
    int defaultLoopsCount;
    private TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> inputHistory;


    public CycledInputLoadingStrategy(ILayersMeta layersMeta, HashMap<IInitInput, InputInitStrategy> externalInputs, int defaultLoopsCount, HashMap<IInitInput, InputStatusMeta> inputStatuses, HashMap<String, ProcessingFrequency> signalProcessingFrequencyMap) {
        epoch = 0L;
        loop = 0;
        this.layersMeta = layersMeta;
        this.externalInputs = externalInputs;
        this.inputStatuses = inputStatuses;
        this.defaultLoopsCount = defaultLoopsCount;
        init(defaultLoopsCount, signalProcessingFrequencyMap);
        neuronInputMapping = new HashMap<>();
        inputHistory = new TreeMap<>();

    }

    private void init(int defaultLoopsCount, HashMap<String, ProcessingFrequency> signalProcessingFrequencyMap) {
        ISignalChain signalChain = new CycleSignalsProcessingChain();
        List<INeuron> neurons = new LinkedList<>();
        CycleNeuron cycleNeuron = new CycleNeuron(defaultLoopsCount, signalChain, 0l, epoch);
        HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequency = cycleNeuron.getInputProcessingFrequencyHashMap();
        for (IInitInput initInput : inputStatuses.keySet()) {
            inputProcessingFrequency.put(initInput, new ProcessingFrequency(initInput.getDefaultProcessingFrequency().getEpoch(), initInput.getDefaultProcessingFrequency().getLoop()));
        }
        cycleNeuron.setInputProcessingFrequencyHashMap(inputProcessingFrequency);
        HashMap<Class<? extends ISignal>, ProcessingFrequency> signalProcessingFrequencyMapIn = new HashMap<>();
        for (String className : signalProcessingFrequencyMap.keySet()) {
            try {
                signalProcessingFrequencyMapIn.put((Class<? extends ISignal>) Class.forName(className), signalProcessingFrequencyMap.get(className));
            } catch (ClassNotFoundException e) {
                logger.error("Cannot find configuration class in jar:", e);
                throw new ConfigurationClassMissedException(e.getMessage());
            }
        }
        cycleNeuron.setSignalProcessingFrequencyMap(signalProcessingFrequencyMapIn);
        neurons.add(cycleNeuron);
        ILayerMeta layerMeta = new InMemoryLayerMeta(Integer.MIN_VALUE, neurons, new HashMap<>());
        layersMeta.addLayerMeta(layerMeta, 0);

    }

    @Override
    public Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses) {
        signalsPersistStorage.cleanOutdatedSignals();
        if (layersMeta.getLayerByPosition(Integer.MIN_VALUE) != null) {
            CycleNeuron cl = ((CycleNeuron) layersMeta.getLayerByPosition(Integer.MIN_VALUE).getNeuronByID(0l));
            HashMap<Class<? extends ISignal>, ProcessingFrequency> frequencyHashMap = cl.getSignalProcessingFrequencyMap();

            HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequencyHashMap = cl.getInputProcessingFrequencyHashMap();

            if (loop >= cl.getLoopCount()) {
                for (IInitInput iii : inputStatuses.keySet()) {
                    ProcessingFrequency ipf = null;
                    if (inputProcessingFrequencyHashMap.containsKey(iii)) {
                        ipf = inputProcessingFrequencyHashMap.get(iii);
                    }
                    if (inputStatuses.get(iii).getCurrentRuns() %
                            cl.getLoopCount() == 0 && (ipf != null && (ipf.getLoop() != null && loop % ipf.getLoop() == 0) || (ipf.getEpoch() != null && epoch % ipf.getEpoch() == 0))) {
                        List<ISignal> signals = new LinkedList<>();
                        List<IInputSignal> signalsHistory = new LinkedList<>();
                        for (IInputSignal signal : iii.readSignals()) {
                            ProcessingFrequency pf = frequencyHashMap.get(signal.getCurrentSignalClass());
                            if (loop % pf.getLoop() == 0 && epoch % pf.getEpoch() == 0) {
                                signal.setInnerLoop(defaultLoopsCount);
                                signal.setEpoch(epoch);
                                signal.setLoop(loop);
                                signalsHistory.add((IInputSignal) signal.copySignal());
                                signals.add(signal);
                            }
                        }
                        signalsPersistStorage.putSignals(externalInputs.get(iii).getInputs(layersMeta, signals));
                        inputStatuses.get(iii).setCurrentRuns(0);
                        inputStatuses.get(iii).setBeenUsed(true);
                        if (inputHistory.containsKey(epoch)) {
                            if (inputHistory.get(epoch).containsKey(loop)) {
                                inputHistory.get(epoch).get(loop).addAll(signalsHistory);
                            } else {
                                TreeMap<Integer, List<IInputSignal>> history = new TreeMap<>();
                                history.put(loop, signalsHistory);
                                inputHistory.put(epoch, history);
                            }
                        } else {
                            TreeMap<Integer, List<IInputSignal>> history = new TreeMap<>();
                            history.put(loop, signalsHistory);
                            inputHistory.put(epoch, history);
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
        return loop;
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
        ILayerMeta layerMeta = layersMeta.getLayerByPosition(Integer.MIN_VALUE);
        if (layerMeta == null) {
            List<INeuron> neurons = new LinkedList<>();
            CycleNeuron cycleNeuron = new CycleNeuron(defaultLoopsCount, signalChain, neuronId, epoch);
            HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequency = cycleNeuron.getInputProcessingFrequencyHashMap();
            for (IInitInput initInput : inputStatuses.keySet()) {
                inputProcessingFrequency.put(initInput, new ProcessingFrequency(initInput.getDefaultProcessingFrequency().getEpoch(), initInput.getDefaultProcessingFrequency().getLoop()));
            }
            cycleNeuron.setInputProcessingFrequencyHashMap(inputProcessingFrequency);
            neurons.add(cycleNeuron);
            layerMeta = new InMemoryLayerMeta(Integer.MIN_VALUE, neurons, new HashMap<>());
            layersMeta.addLayerMeta(layerMeta, 0);
        } else {
            CycleNeuron cycleNeuron = (CycleNeuron) layerMeta.getNeuronByID(0l);
            HashMap<IInitInput, ProcessingFrequency> inputProcessingFrequency = cycleNeuron.getInputProcessingFrequencyHashMap();
            for (IInitInput initInput : inputStatuses.keySet()) {
                inputProcessingFrequency.put(initInput, new ProcessingFrequency(initInput.getDefaultProcessingFrequency().getEpoch(), initInput.getDefaultProcessingFrequency().getLoop()));
            }
            cycleNeuron.setInputProcessingFrequencyHashMap(inputProcessingFrequency);
        }

    }

    @Override
    public void registerInput(IInitInput initInput, InputInitStrategy initStrategy) {

    }


    @Override
    public TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> getInputHistory() {
        return inputHistory;
    }
}
