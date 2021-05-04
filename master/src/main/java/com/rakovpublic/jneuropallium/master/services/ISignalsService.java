package com.rakovpublic.jneuropallium.master.services;

import java.util.HashMap;
import java.util.List;

public interface ISignalsService {
    void saveSignal(String signalJson, Integer layerId, Long neuronId);

    List<String> loadSignals(HashMap<Integer, List<Long>> targets);
}
