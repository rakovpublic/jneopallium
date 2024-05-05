/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronC extends Neuron implements NeuronWithDoubleField {
    protected Double fieldDouble;

    @Override
    public Double getDoubleField() {
        return fieldDouble;
    }

    @Override
    public void setDoubleField(Double value) {
        this.fieldDouble = value;
    }

    @Override
    public void activate() {
        super.activate();
        Double d = Math.log(fieldDouble);
        if(d>1){
            result.add(new DoubleSignal(d, getLayer().getId(), getId(), 1, "Double signal", false, this.currentNeuronClass.getName(), false, true, DoubleSignal.class.getName()));
            fieldDouble = 0.0;
        }
    }
}
