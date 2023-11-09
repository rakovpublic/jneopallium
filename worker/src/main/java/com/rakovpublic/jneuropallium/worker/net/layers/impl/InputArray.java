package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import java.util.List;

public class InputArray {
    private List<InputData> inputData;

    public InputArray() {
    }

    public InputArray(List<InputData> inputData) {
        this.inputData = inputData;
    }

    public List<InputData> getInputData() {
        return inputData;
    }

    public void setInputData(List<InputData> inputData) {
        this.inputData = inputData;
    }
}
