/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.ioutils;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.SignalWrapper;
import sun.misc.Signal;

import java.util.List;

public class SignalArray {
    public List<SignalWrapper> signals;

    public SignalArray() {
    }

    public List<SignalWrapper> getSignals() {
        return signals;
    }

    public void setSignals(List<SignalWrapper> signals) {
        this.signals = signals;
    }
}
