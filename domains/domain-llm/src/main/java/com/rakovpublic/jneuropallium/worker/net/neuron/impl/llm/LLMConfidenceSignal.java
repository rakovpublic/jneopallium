/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl.llm;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

/**
 * Signal carrying a verified confidence score and applicability verdict.
 * Emitted by LLMVerificationNeuron after cross-validation.
 * Slow loop, epoch 3 — runs less frequently than query/response signals.
 */
public class LLMConfidenceSignal extends AbstractSignal<LLMConfidenceItem> implements ISignal<LLMConfidenceItem> {

    public LLMConfidenceSignal(LLMConfidenceItem value, Integer sourceLayer, Long sourceNeuron,
                               Integer timeAlive, String description, boolean fromExternalNet,
                               String inputName, boolean needToRemoveDuringLearning,
                               boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet,
                inputName, needToRemoveDuringLearning, needToProcessDuringLearning,
                name, LLMConfidenceSignal.class.getCanonicalName());
        this.loop = 2;
        this.epoch = 3L;
    }

    @Override
    public Class<? extends ISignal<LLMConfidenceItem>> getCurrentSignalClass() {
        return LLMConfidenceSignal.class;
    }

    @Override
    public Class<LLMConfidenceItem> getParamClass() {
        return LLMConfidenceItem.class;
    }

    @Override
    public LLMConfidenceSignal copySignal() {
        return new LLMConfidenceSignal(value, this.getSourceLayerId(), this.getSourceNeuronId(),
                this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName(),
                this.isNeedToRemoveDuringLearning(), this.isNeedToProcessDuringLearning(),
                this.getName());
    }
}
