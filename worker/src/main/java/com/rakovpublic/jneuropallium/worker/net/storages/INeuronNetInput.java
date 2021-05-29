package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.List;

public interface INeuronNetInput {
    void sendCallBack(List<ISignal> signals);
}
