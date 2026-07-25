/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Signal that triggers fallback to internal knowledge when the LLM is unavailable or too slow.
 * Fast loop, epoch 1 — must be detected and acted on immediately.
 */
public class LLMTimeoutSignal extends AbstractSignal<LLMTimeoutItem> implements ISignal<LLMTimeoutItem> {

    public LLMTimeoutSignal(LLMTimeoutItem value, Integer sourceLayer, Long sourceNeuron,
                            Integer timeAlive, String description, boolean fromExternalNet,
                            String inputName, boolean needToRemoveDuringLearning,
                            boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet,
                inputName, needToRemoveDuringLearning, needToProcessDuringLearning,
                name, LLMTimeoutSignal.class.getCanonicalName());
        this.loop = 1;
        this.epoch = 1L;
    }

    @Override
    public Class<? extends ISignal<LLMTimeoutItem>> getCurrentSignalClass() {
        return LLMTimeoutSignal.class;
    }

    @Override
    public Class<LLMTimeoutItem> getParamClass() {
        return LLMTimeoutItem.class;
    }

    @Override
    public LLMTimeoutSignal copySignal() {
        return new LLMTimeoutSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(),
                this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName(),
                this.isNeedToRemoveDuringLearning(), this.isNeedToProcessDuringLearning(),
                this.getName());
    }
}
