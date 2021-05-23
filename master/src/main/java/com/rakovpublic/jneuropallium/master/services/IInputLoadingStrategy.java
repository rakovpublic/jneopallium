package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.master.services.impl.InputStatusMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;

import java.util.HashMap;
import java.util.List;

//interface for cycle running before next run with next input
public interface IInputLoadingStrategy {
    Boolean populateInput(ISignalsPersistStorage signalsPersistStorage, HashMap<IInitInput, InputStatusMeta> inputStatuses);
}
