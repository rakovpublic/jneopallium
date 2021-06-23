package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;

import java.util.HashMap;
import java.util.List;

public interface IInputResolver {
    void registerInput(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns);
    ISignalsPersistStorage getSignalPersistStorage();
    ISignalHistoryStorage getSignalsHistoryStorage();
    HashMap<String,Long> getCycleNeuronAddressMapping();
    Long getCurrentLoop();
    void saveHistory();
    void populateInput();
    IResultSignal getDesiredResult();
}
