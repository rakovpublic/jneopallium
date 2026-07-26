package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.InitInputWrapper;
import com.rakovpublic.jneuropallium.worker.net.signals.InputInitStrategyWrapper;


public class InputData {
    private InitInputWrapper iInputSource;
    private boolean isMandatory;
    private InputInitStrategyWrapper initStrategy;
    private Integer amountOfRuns;

    public InputData() {
    }

    public InputData(InitInputWrapper iInputSource, boolean isMandatory, InputInitStrategyWrapper initStrategy, Integer amountOfRuns) {
        this.iInputSource = iInputSource;
        this.isMandatory = isMandatory;
        this.initStrategy = initStrategy;
        this.amountOfRuns = amountOfRuns;
    }

    public InitInputWrapper getiInputSource() {
        return iInputSource;
    }

    public void setiInputSource(InitInputWrapper iInputSource) {
        this.iInputSource = iInputSource;
    }

    public boolean isMandatory() {
        return isMandatory;
    }

    public void setMandatory(boolean mandatory) {
        isMandatory = mandatory;
    }

    public InputInitStrategyWrapper getInitStrategy() {
        return initStrategy;
    }

    public void setInitStrategy(InputInitStrategyWrapper initStrategy) {
        this.initStrategy = initStrategy;
    }

    public Integer getAmountOfRuns() {
        return amountOfRuns;
    }

    public void setAmountOfRuns(Integer amountOfRuns) {
        this.amountOfRuns = amountOfRuns;
    }
}
