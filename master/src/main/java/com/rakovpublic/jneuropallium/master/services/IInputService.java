package com.rakovpublic.jneuropallium.master.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.IInputMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;

import java.util.HashMap;
import java.util.List;

public interface IInputService {
    void inputSourceUpdated(String name);

    void register( IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy,Integer amountOfRunsToUpdate);

    void uploadWorkerResult(String name, HashMap<Integer, HashMap<Long, List<ISignal>>> signals);

    ISplitInput getNext(String name);

    boolean hasNextComplete();

    boolean hasPrepared();

    void prepareInputs();
}
