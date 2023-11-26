/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.neuron.IResultNeuron;

import java.util.List;

public interface ResultInterpreter {
    List<Result> getResult(List<IResultNeuron> neurons);
}
