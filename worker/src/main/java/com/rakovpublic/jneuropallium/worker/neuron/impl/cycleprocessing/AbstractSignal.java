package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public abstract class AbstractSignal<T> implements ISignal<T> {
    protected T value;
    private Integer sourceLayer;
    private Long sourceNeuron;
    private Integer timeAlive;
    private String description;
    private boolean fromExternalNet;
    private String inputName;

    private Integer currentInnerLoop;

    private Integer innerLoop;

    private boolean needToRemoveDuringLearning;

    @Override
    public boolean isNeedToProcessDuringLearning() {
        return needToProcessDuringLearning;
    }

    private boolean needToProcessDuringLearning;

    @Override
    public String getInputName() {
        return inputName;
    }

    @Override
    public void setInputName(String inputName) {
        this.inputName = inputName;
    }

    @Override
    public boolean isFromExternalNet() {
        return this.fromExternalNet;
    }

    @Override
    public void setFromExternalNet(boolean fromExternalNet) {
        this.fromExternalNet = fromExternalNet;
    }

    public AbstractSignal(T value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning) {
        this.value = value;
        this.sourceLayer = sourceLayer;
        this.sourceNeuron = sourceNeuron;
        this.timeAlive = timeAlive;
        this.description = description;
        this.fromExternalNet = fromExternalNet;
        this.inputName = inputName;
        this.needToRemoveDuringLearning = needToRemoveDuringLearning;
        this.needToProcessDuringLearning = needToProcessDuringLearning;
    }

    @Override
    public T getValue() {
        return value;
    }


    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean canUseProcessorForParent() {
        return false;
    }

    @Override
    public ISignal<T> prepareSignalToNextStep() {
        if (timeAlive > 0) {
            this.timeAlive -= 1;
        }
        return this;
    }

    @Override
    public int getSourceLayerId() {
        return sourceLayer;
    }

    @Override
    public void setSourceLayerId(int layerId) {
        sourceLayer = layerId;
    }

    @Override
    public Long getSourceNeuronId() {
        return sourceNeuron;
    }

    @Override
    public void setSourceNeuronId(Long neuronId) {
        sourceNeuron = neuronId;
    }

    @Override
    public int getTimeAlive() {
        return timeAlive;
    }

    @Override
    public boolean isNeedToRemoveDuringLearning() {
        return needToRemoveDuringLearning;
    }

    @Override
    public void setNeedToRemoveDuringLearning(boolean value) {
        needToRemoveDuringLearning = value;
    }

    @Override
    public Integer getCurrentInnerLoop() {
        return currentInnerLoop;
    }

    @Override
    public void setCurrentInnerLoop(Integer currentInnerLoop) {
        this.currentInnerLoop = currentInnerLoop;
    }

    @Override
    public Integer getInnerLoop() {
        return innerLoop;
    }

    @Override
    public void setInnerLoop(Integer innerLoop) {
        this.innerLoop = innerLoop;
    }
}
