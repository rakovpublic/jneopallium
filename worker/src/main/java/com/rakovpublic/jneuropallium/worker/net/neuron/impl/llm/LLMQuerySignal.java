/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Signal that dispatches an advisory query to the LLM endpoint.
 * Slow loop, epoch 2 — never runs on the fast sensorimotor loop.
 * Set loop=2 and epoch=2 on instances before injecting into the network.
 */
public class LLMQuerySignal extends AbstractSignal<LLMQueryItem> implements ISignal<LLMQueryItem> {

    public LLMQuerySignal(LLMQueryItem value, Integer sourceLayer, Long sourceNeuron,
                          Integer timeAlive, String description, boolean fromExternalNet,
                          String inputName, boolean needToRemoveDuringLearning,
                          boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet,
                inputName, needToRemoveDuringLearning, needToProcessDuringLearning,
                name, LLMQuerySignal.class.getCanonicalName());
        this.loop = 2;
        this.epoch = 2L;
    }

    @Override
    public Class<? extends ISignal<LLMQueryItem>> getCurrentSignalClass() {
        return LLMQuerySignal.class;
    }

    @Override
    public Class<LLMQueryItem> getParamClass() {
        return LLMQueryItem.class;
    }

    @Override
    public LLMQuerySignal copySignal() {
        return new LLMQuerySignal(value, this.getSourceLayerId(), this.getSourceNeuronId(),
                this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName(),
                this.isNeedToRemoveDuringLearning(), this.isNeedToProcessDuringLearning(),
                this.getName());
    }
}
