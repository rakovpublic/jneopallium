package com.rakovpublic.jneuropallium.master.model;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public class UploadSignalsRequest {
    private String name;
    private HashMap<Integer, HashMap<Long, List<ISignal>>> signals;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public HashMap<Integer, HashMap<Long, List<ISignal>>> getSignals() {
        return signals;
    }

    public void setSignals(HashMap<Integer, HashMap<Long, List<ISignal>>> signals) {
        this.signals = signals;
    }
}
