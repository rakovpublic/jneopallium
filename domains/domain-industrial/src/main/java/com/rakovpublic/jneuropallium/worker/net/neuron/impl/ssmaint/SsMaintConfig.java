/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;

/**
 * Module-level invariants for the self-supervised maintenance model. The model
 * is trained without labels and keeps learning online from operator feedback,
 * but it is and remains advisory: {@code advisoryOnly} cannot be disabled, so no
 * neuron in this package may ever actuate a device.
 */
public class SsMaintConfig {

    private final boolean labelFree = true;
    private final boolean continuousLearning = true;
    private boolean advisoryOnly = true;

    public boolean isLabelFree() { return labelFree; }
    public boolean isContinuousLearning() { return continuousLearning; }
    public boolean isAdvisoryOnly() { return advisoryOnly; }

    /** Advisory posture is a safety invariant; attempting to disable it fails. */
    public void setAdvisoryOnly(boolean advisoryOnly) {
        if (!advisoryOnly) {
            throw new IllegalArgumentException("self-supervised maintenance model is advisory-only");
        }
        this.advisoryOnly = true;
    }
}
