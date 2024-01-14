/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface NeuronIntField extends INeuron {
    Integer getIntField();
    void setIntField(Integer field);
}
