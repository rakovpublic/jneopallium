/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronA extends Neuron implements NeuronIntField, NeuronWithDoubleField {

    private Integer intField;
    private Double doubleField;




    @Override
    public Integer getIntField() {
        return intField;
    }

    @Override
    public void setIntField(Integer field) {
        this.intField = field;
    }

    @Override
    public Double getDoubleField() {
        return doubleField;
    }

    @Override
    public void setDoubleField(Double value) {
        this.doubleField = value;
    }
}
