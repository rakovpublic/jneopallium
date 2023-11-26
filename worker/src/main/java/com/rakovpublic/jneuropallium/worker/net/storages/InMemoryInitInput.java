/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.storages;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;

import java.util.List;

public interface InMemoryInitInput extends IInitInput {
    void putSignals(List<IInputSignal> signals);
}
