package com.rakovpublic.services;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInputMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.InputInitStrategy;
import com.rakovpublic.model.IInputSource;

import java.util.HashMap;
import java.util.List;

public interface IInputService {
    void register(String name, IInputMeta iInputSource, boolean isMandatory, InputInitStrategy initStrategy);
    void inputUpdated(String name, Integer nextLayerId, HashMap<Long,List<ISignal>> signals);
    ISplitInput getNext(String name);
    boolean hasNextComplete();
}
