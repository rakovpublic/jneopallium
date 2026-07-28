/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Signal carrying a raw (unverified) LLM response back to the network.
 * Slow loop, epoch 2. Must be routed through LLMVerificationNeuron before use.
 */
public class LLMResponseSignal extends AbstractSignal<LLMResponseItem> implements ISignal<LLMResponseItem> {

    public LLMResponseSignal(LLMResponseItem value, Integer sourceLayer, Long sourceNeuron,
                             Integer timeAlive, String description, boolean fromExternalNet,
                             String inputName, boolean needToRemoveDuringLearning,
                             boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet,
                inputName, needToRemoveDuringLearning, needToProcessDuringLearning,
                name, LLMResponseSignal.class.getCanonicalName());
        this.loop = 2;
        this.epoch = 2L;
    }

    @Override
    public Class<? extends ISignal<LLMResponseItem>> getCurrentSignalClass() {
        return LLMResponseSignal.class;
    }

    @Override
    public Class<LLMResponseItem> getParamClass() {
        return LLMResponseItem.class;
    }

    @Override
    public LLMResponseSignal copySignal() {
        return new LLMResponseSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(),
                this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName(),
                this.isNeedToRemoveDuringLearning(), this.isNeedToProcessDuringLearning(),
                this.getName());
    }
}
