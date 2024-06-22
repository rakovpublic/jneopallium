/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;

import java.util.HashMap;
import java.util.List;
@JsonDeserialize(using = InitInputDeserializer.class)
public class InitInputWrapper  {
    public IInitInput initInput;
    public Class<? extends IInitInput> clazz;

    public IInitInput getInitInput() {
        return initInput;
    }

    public void setInitInput(IInitInput initInput) {
        this.initInput = initInput;
    }

    public Class<? extends IInitInput> getClazz() {
        return clazz;
    }

    public void setClazz(Class<? extends IInitInput> clazz) {
        this.clazz = clazz;
    }

    public InitInputWrapper() {
    }

    public InitInputWrapper(IInitInput initInput) {
        this.initInput = initInput;
        clazz = initInput.getClass();
    }

}
