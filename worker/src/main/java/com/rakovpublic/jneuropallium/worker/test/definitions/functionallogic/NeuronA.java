/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class NeuronA extends Neuron implements IResultNeuron<TestResultSignal>,NeuronIntField, NeuronWithDoubleField {

    public Integer intField;
    public Double doubleField;

    public NeuronA() {
        super();
        intField =0;
        doubleField=0d;
        currentNeuronClass = NeuronA.class;
        resultClasses.add(ASignal.class);
    }

    public NeuronA(Long neuronId, ISignalChain processingChain, Long run) {
        super(neuronId, processingChain, run);
        currentNeuronClass = NeuronA.class;
        intField =0;
        doubleField=0d;
        resultClasses.add(ASignal.class);
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
    public Double getDoubleField() {
        return doubleField;
    }

    @Override
    public void setDoubleField(Double value) {
        this.doubleField = value;
    }

    @Override
    public void activate() {

        super.activate();
        Double d = doubleField%intField;
        if(d>=1){
            result.add( new ASignal(d.intValue(), getLayer().getId(), getId(), 1, "a signal", false, this.currentNeuronClass.getName(), false, true, ASignal.class.getName()));
            intField = 0;
            doubleField = 0.0;
        }
    }

    @Override
    public TestResultSignal getFinalResult() {

        String res = result.toString();
        TestResultSignal resultSignal = new TestResultSignal(res, this.getLayer().getId(), this.getId(), 1, "", false, this.getClass().getCanonicalName(), false, false, "");
        return resultSignal;
    }
}
