package com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Minimal concrete signal for unit tests.
 */
public class TestSignal extends AbstractSignal<Double> {

    public TestSignal(int timeAlive, boolean needToRemoveDuringLearning) {
        this.timeAlive = timeAlive;
        this.needToRemoveDuringLearning = needToRemoveDuringLearning;
        this.value = 1.0;
        this.sourceLayer = 0;
        this.sourceNeuron = 0L;
        this.description = "test";
        this.fromExternalNet = false;
        this.inputName = "test";
        this.needToProcessDuringLearning = false;
        this.name = "test";
        this.epoch = 0L;
        this.loop = 0;
        this.currentClassName = TestSignal.class.getName();
    }

    @Override
    public Class<? extends ISignal<Double>> getCurrentSignalClass() {
        return (Class<? extends ISignal<Double>>) (Class<?>) TestSignal.class;
    }

    @Override
    public Class<Double> getParamClass() {
        return Double.class;
    }

    @Override
    public <K extends ISignal<Double>> K copySignal() {
        return (K) new TestSignal(timeAlive, needToRemoveDuringLearning);
    }
}
