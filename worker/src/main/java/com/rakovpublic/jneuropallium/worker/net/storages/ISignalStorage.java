package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.io.Serializable;
import java.util.List;

//refactor signals map to this object;
public interface ISignalStorage extends Serializable {
    List<ISignal> getSignalsForNeuron(Long neuronId);
}
