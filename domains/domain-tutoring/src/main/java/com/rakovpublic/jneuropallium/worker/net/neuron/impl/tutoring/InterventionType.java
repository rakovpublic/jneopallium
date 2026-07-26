/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.tutoring;

/**
 * A higher-level intervention a guard neuron may recommend when moment-
 * to-moment state indicates the learner should pause, be encouraged, or be
 * handed off to a human.
 */
public enum InterventionType {
    BREAK, ENCOURAGE, REDIRECT, ESCALATE_TO_HUMAN
}
