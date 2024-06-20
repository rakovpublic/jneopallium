/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class DummyWeight<K extends ISignal> implements IWeight<K, DummyChangeWeightSignal> {
    public String signalClass = "com.rakovpublic.jneuropallium.worker.net.signals.ISignal";
    public String weightClass = "com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic.DummyWeight";

    public DummyWeight() {
    }

    public String getWeightClass() {
        return weightClass;
    }

    public void setWeightClass(String weightClass) {
        this.weightClass = weightClass;
    }


    @Override
    public K process(K signal) {
        return signal;
    }

    @Override
    public void changeWeight(DummyChangeWeightSignal signal) {

    }

    @Override
    public Class getSignalClass() {
        return ISignal.class;
    }
}
