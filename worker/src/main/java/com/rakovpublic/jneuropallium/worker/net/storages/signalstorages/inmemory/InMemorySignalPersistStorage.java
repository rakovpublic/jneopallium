package com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalsPersistStorage;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

public class InMemorySignalPersistStorage implements ISignalsPersistStorage {
    private TreeMap<Integer, HashMap<Long, List<ISignal>>> signals;

    public InMemorySignalPersistStorage() {
        this.signals = new TreeMap<>();
    }

    @Override
    public void putSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> signalsInput) {
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
    public HashMap<Long, List<ISignal>> getLayerSignals(Integer layerId) {
        return signals.get(layerId);
    }

    @Override
    public void cleanOutdatedSignals() {
        for (Integer layerId : signals.keySet()) {
            for (Long neuron : signals.get(layerId).keySet()) {
                List<ISignal> neuronSignal = signals.get(layerId).get(neuron);
                for (ISignal signal : neuronSignal) {
                    neuronSignal.remove(signal);
                    neuronSignal.add(signal.prepareSignalToNextStep());
                }
            }
        }

    }

    @Override
    public void cleanMiddleLayerSignals() {
        boolean first = true;
        for(Integer layerId: signals.keySet()){
            if(layerId == Integer.MIN_VALUE){
                continue;
            }
            if(first){
                first =false;
                continue;
            }
            signals.put(layerId, new HashMap<>());

        }
    }

    @Override
    public TreeMap<Integer, HashMap<Long, List<ISignal>>> getAllSignals() {
        return signals;
    }

    @Override
    public void deletedLayerInput(Integer deletedLayerId) {
      signals.remove(deletedLayerId);
    }

}
