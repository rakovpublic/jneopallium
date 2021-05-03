package com.rakovpublic.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInputMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;
import com.rakovpublic.model.IInputSource;

import java.util.HashMap;
import java.util.List;

public interface IInputService {
    void inputSourceUpdated(String name);
    void register(String name, IInputMeta iInputSource, boolean isMandatory, InputInitStrategy initStrategy);
    void uploadWorkerResult(String name,  HashMap<Long,List<ISignal>> signals);
    ISplitInput getNext(String name);
    boolean hasNextComplete();
}
