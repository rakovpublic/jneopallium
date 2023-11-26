/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.storages.InMemoryInitInput;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class InMemoryInitInputImpl implements InMemoryInitInput {
    private List<IInputSignal> signals;
    private String name;

    public InMemoryInitInputImpl(String name) {
        this.name = name;
        signals = new LinkedList<>();
    }

    @Override
    public void putSignals(List<IInputSignal> signals) {
        this.signals = signals;
    }

    @Override
    public List<IInputSignal> readSignals() {
        List<IInputSignal> res = new LinkedList<>();
        res.addAll(signals);
        signals.clear();
        return res;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() {
        return new HashMap<>();
    }
}
