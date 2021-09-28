package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface ISignalHistoryStorage {
    List<ISignal> getSourceSignalsForRun(Long nRun, NeuronAddress forTarget);
    void save(HashMap<Integer,HashMap<Long,List<ISignal>>> history,Long run);
}
