/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

public abstract class AbstractSignal<T> implements ISignal<T> {
    public T value;
    public Integer sourceLayer;
    public Long sourceNeuron;
    public Integer timeAlive;
    public String description;
    public boolean fromExternalNet;
    public String inputName;

    public Integer currentInnerLoop;
    public Long epoch;
    public Integer innerLoop;
    public Integer loop;
    public String currentClassName;

    public boolean needToRemoveDuringLearning;

    public String name;

    @Override
    public boolean isNeedToProcessDuringLearning() {
        return needToProcessDuringLearning;
    }

    public boolean needToProcessDuringLearning;

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

    public AbstractSignal() {
    }

    public AbstractSignal(T value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name, String currentClassName) {
        this.value = value;
        this.sourceLayer = sourceLayer;
        this.sourceNeuron = sourceNeuron;
        this.timeAlive = timeAlive;
        this.description = description;
        this.fromExternalNet = fromExternalNet;
        this.inputName = inputName;
        this.needToRemoveDuringLearning = needToRemoveDuringLearning;
        this.needToProcessDuringLearning = needToProcessDuringLearning;
        this.name = name;
        this.epoch = 0l;
        this.loop = 0;
        this.currentClassName = currentClassName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
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

    @Override
    public Long getEpoch() {
        return epoch;
    }

    @Override
    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    @Override
    public Integer getLoop() {
        return loop;
    }

    @Override
    public void setLoop(Integer loop) {
        this.loop = loop;
    }
}
