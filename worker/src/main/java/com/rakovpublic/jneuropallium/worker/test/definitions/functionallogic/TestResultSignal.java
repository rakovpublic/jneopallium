package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class TestResultSignal extends AbstractSignal<String> implements IResultSignal<String> {
    public TestResultSignal(String value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name,TestResultSignal.class.getCanonicalName());
    }

    @Override
    public String getResultObject() {
        return value;
    }

    @Override
    public Class<String> getResultObjectClass() {
        return String.class;
    }

    @Override
    public Class<? extends ISignal<String>> getCurrentSignalClass() {
        return TestResultSignal.class;
    }

    @Override
    public Class<String> getParamClass() {
        return String.class;
    }

    @Override
    public TestResultSignal copySignal() {
        return new TestResultSignal (value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }
}
