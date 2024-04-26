/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronB extends Neuron implements NeuronIntField {
    private  Integer intField;
    @Override
    public Integer getIntField() {
        return intField;
    }

    @Override
    public void setIntField(Integer field) {
        this.intField = field;
    }
}
