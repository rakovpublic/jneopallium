package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface ISignalHistoryStorage {
    HashMap<Long, List<ISignal>> getSourceSignalsForRun(Long nRun);
}
