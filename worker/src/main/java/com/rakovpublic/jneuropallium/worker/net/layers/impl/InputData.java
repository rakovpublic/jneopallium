package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.InputInitStrategy;

public class InputData {
    private IInitInput iInputSource;
    private boolean isMandatory;
    private InputInitStrategy initStrategy;
    private Integer amountOfRuns;

    public InputData(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy, Integer amountOfRuns) {
        this.iInputSource = iInputSource;
        this.isMandatory = isMandatory;
        this.initStrategy = initStrategy;
        this.amountOfRuns = amountOfRuns;
    }

    public IInitInput getiInputSource() {
        return iInputSource;
    }

    public void setiInputSource(IInitInput iInputSource) {
        this.iInputSource = iInputSource;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public InputInitStrategy getInitStrategy() {
        return initStrategy;
    }

    public void setInitStrategy(InputInitStrategy initStrategy) {
        this.initStrategy = initStrategy;
    }

    public Integer getAmountOfRuns() {
        return amountOfRuns;
    }

    public void setAmountOfRuns(Integer amountOfRuns) {
        this.amountOfRuns = amountOfRuns;
    }
}
