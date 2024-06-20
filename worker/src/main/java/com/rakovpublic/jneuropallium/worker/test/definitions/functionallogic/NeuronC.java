/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronC extends Neuron implements NeuronWithDoubleField {
    public Double fieldDouble;

    public NeuronC() {
        super();
        fieldDouble = 0d;
        currentNeuronClass = NeuronC.class;
        resultClasses.add(DoubleSignal.class);
    }

    public NeuronC(Long neuronId, ISignalChain processingChain, Long run) {
        super(neuronId, processingChain, run);
        currentNeuronClass = NeuronC.class;
        fieldDouble = 0d;
        resultClasses.add(DoubleSignal.class);
    }

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
