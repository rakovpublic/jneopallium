/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = SignalDeserializer.class)
public class SignalWrapper {
    public IInputSignal signal;

    public SignalWrapper() {
    }

    public SignalWrapper(IInputSignal signal) {
        this.signal = signal;
    }

    public IInputSignal getSignal() {
        return signal;
    }

    public void setSignal(IInputSignal signal) {
        this.signal = signal;
    }
}
