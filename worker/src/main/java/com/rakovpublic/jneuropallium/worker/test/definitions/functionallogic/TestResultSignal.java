package com.rakovpublic.jneuropallium.worker.test.definitions.functionallogic;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class TestResultSignal extends AbstractSignal<String> implements IResultSignal<String> {
    public TestResultSignal(String value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, name);
    }

    @Override
    public String getResultObject() {
        return null;
    }

    @Override
    public Class<String> getResultObjectClass() {
        return null;
    }

    @Override
    public Class<? extends ISignal<String>> getCurrentSignalClass() {
        return null;
    }

    @Override
    public Class<String> getParamClass() {
        return null;
    }

    @Override
    public <K extends ISignal<String>> K copySignal() {
        return null;
    }
}
