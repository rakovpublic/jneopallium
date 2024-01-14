/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronA extends Neuron implements NeuronIntField, NeuronWithDoubleField {
    @Override
    public Integer getIntField() {
        return null;
    }

    @Override
    public void setIntField(Integer field) {

    }

    @Override
    public Double getDoubleField() {
        return null;
    }

    @Override
    public void setDoubleField() {

    }
}
