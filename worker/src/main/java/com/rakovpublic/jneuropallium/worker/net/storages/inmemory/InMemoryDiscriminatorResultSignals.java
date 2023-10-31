/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.storages.INeuronNetInput;

import java.util.HashMap;
import java.util.List;

//TODO: implement
public class InMemoryDiscriminatorResultSignals implements IInitInput {
    @Override
    public List<IInputSignal> readSignals() {
        return null;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public INeuronNetInput getNeuronNetInput() {
        return null;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return null;
    }
}
