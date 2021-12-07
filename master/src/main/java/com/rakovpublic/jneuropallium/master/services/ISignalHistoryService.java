package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface ISignalHistoryService extends Service{
    void saveSignals(Integer run, Integer layerId, HashMap<Long, List<ISignal>> signals);

    List<ISignal> loadSignals(Integer run, Integer layerId, Long neuronId);
}
