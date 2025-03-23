/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalsPersistStorage;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemorySignalPersistStorage implements ISignalsPersistStorage {
    private final TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signals;

    public InMemorySignalPersistStorage() {
        this.signals = new TreeMap<>();
    }

    @Override
    public void putSignals(HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> signalsInput) {
        for (Integer layerId : signalsInput.keySet()) {
            if (signals.containsKey(layerId)) {
                for (Long neuronId : signalsInput.get(layerId).keySet()) {
                    if (signals.get(layerId).containsKey(neuronId)) {
                        signals.get(layerId).get(neuronId).addAll(signalsInput.get(layerId).get(neuronId));
                    } else {
                        signals.get(layerId).put(neuronId, signalsInput.get(layerId).get(neuronId));
                    }
                }
            } else {
                signals.put(layerId, signalsInput.get(layerId));
            }
        }

    }

    @Override
    public HashMap<Long, CopyOnWriteArrayList<ISignal>> getLayerSignals(Integer layerId) {
        return signals.get(layerId);
    }

    @Override
    public void cleanOutdatedSignals() {
        for (Integer layerId : signals.keySet()) {
            for (Long neuron : signals.get(layerId).keySet()) {
                List<ISignal> neuronSignal = signals.get(layerId).get(neuron);
                for (ISignal signal : neuronSignal) {
                    neuronSignal.remove(signal);
                    if (signal.getTimeAlive() >= 1) {
                        neuronSignal.add(signal.prepareSignalToNextStep());
                    }
                }
            }
        }

    }

    @Override
    public void cleanMiddleLayerSignals() {
        boolean first = true;
        for (Integer layerId : signals.keySet()) {
            if (layerId == Integer.MIN_VALUE) {
                continue;
            }
            if (first) {
                first = false;
                continue;
            }
            for (Long neuron : signals.get(layerId).keySet()) {
                List<ISignal> neuronSignal = signals.get(layerId).get(neuron);
                for (ISignal signal : neuronSignal) {
                    if (signal.isNeedToRemoveDuringLearning() || signal.getTimeAlive() < 1) {
                        neuronSignal.remove(signal);
                    }
                }
            }

        }
    }

    @Override
    public TreeMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> getAllSignals() {
        return signals;
    }

    @Override
    public void deletedLayerInput(Integer deletedLayerId) {
        signals.remove(deletedLayerId);
    }

    @Override
    public boolean hasSignalsToProcess() {
        for (Integer layerId : signals.keySet()) {
            for (Long neuronId : signals.get(layerId).keySet()) {
                if (signals.get(layerId).get(neuronId).size() > 0) {
                    return true;
                }
            }
        }
        return false;
    }

}
