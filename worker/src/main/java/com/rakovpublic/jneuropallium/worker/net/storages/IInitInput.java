package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface IInitInput {
    List<ISignal> readSignals();
    String getName();
    INeuronNetInput getNeuronNetInput();
    HashMap<String, List<IResultSignal>> getDesiredResults();
}
