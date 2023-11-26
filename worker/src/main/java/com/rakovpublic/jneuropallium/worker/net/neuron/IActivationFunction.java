package com.rakovpublic.jneuropallium.worker.net.neuron;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.Optional;

public interface IActivationFunction<I extends ISignal> {
    Optional<I> activate(I activate);
}
