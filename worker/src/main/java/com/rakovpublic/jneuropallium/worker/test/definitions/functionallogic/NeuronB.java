/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronB extends Neuron implements NeuronIntField {
    public  Integer intField;

    public NeuronB() {
        super();
        intField=0;
        currentNeuronClass= NeuronB.class;
        resultClasses.add(IntSignal.class);
        resultClasses.add(DoubleSignal.class);
    }

    public NeuronB(Long neuronId, ISignalChain processingChain, Long run) {
        super(neuronId, processingChain, run);
        intField=0;
        resultClasses.add(IntSignal.class);
        currentNeuronClass= NeuronB.class;
        resultClasses.add(DoubleSignal.class);
    }

    @Override
    public Integer getIntField() {
        return intField;
    }

    @Override
    public void setIntField(Integer field) {
        this.intField = field;
    }

    @Override
    public void activate() {
        super.activate();
        Double d = Math.log(intField);
        if(d>1){
            result.add(new DoubleSignal(d, getLayer().getId(), getId(), 1, "Double signal", false, this.currentNeuronClass.getName(), false, true, DoubleSignal.class.getName()));
            result.add(new IntSignal(d.intValue(), getLayer().getId(), getId(), 1, "Integer signal", false, this.currentNeuronClass.getName(), false, true, IntSignal.class.getName()));
            intField = 0;
        }
    }
}
