/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(using = InputInitStrategyDeserializer.class)
public class InputInitStrategyWrapper {
    public InputInitStrategy iNeuronNetInput;
    public Class<? extends InputInitStrategy> clazz;

    public InputInitStrategyWrapper(InputInitStrategy iNeuronNetInput) {
        this.iNeuronNetInput = iNeuronNetInput;
        clazz = iNeuronNetInput.getClass();
    }

    public InputInitStrategyWrapper() {
    }

    public InputInitStrategy getiNeuronNetInput() {
        return iNeuronNetInput;
    }

    public void setiNeuronNetInput(InputInitStrategy iNeuronNetInput) {
        this.iNeuronNetInput = iNeuronNetInput;
    }

    public Class<? extends InputInitStrategy> getClazz() {
        return clazz;
    }

    public void setClazz(Class<? extends InputInitStrategy> clazz) {
        this.clazz = clazz;
    }
}
