package com.rakovpublic.jneopalium.model.brain.signals;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class Gamma extends AbstractSignal<Double> {
    @Override
    public Class<? extends ISignal<Double>> getCurrentSignalClass() {
        return null;
    }

    @Override
    public Class<Double> getParamClass() {
        return null;
    }

    @Override
    public <K extends ISignal<Double>> K copySignal() {
        return null;
    }
}
