package com.rakovpublic.jneuropallium.worker.model;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public class UploadSignalsRequest {
    private String name;
    private HashMap<Integer, HashMap<Long, List<ISignal>>> signals;
    private boolean discriminator;
    private String discriminatorName;

    public boolean isDiscriminator() {
        return discriminator;
    }

    public void setDiscriminator(boolean discriminator) {
        this.discriminator = discriminator;
    }

    public String getDiscriminatorName() {
        return discriminatorName;
    }

    public void setDiscriminatorName(String discriminatorName) {
        this.discriminatorName = discriminatorName;
    }

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
