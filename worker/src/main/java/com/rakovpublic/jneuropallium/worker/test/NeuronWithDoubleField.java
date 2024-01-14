/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.test;

import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;

public interface NeuronWithDoubleField extends INeuron  {
    Double getDoubleField();
    void setDoubleField();
}
