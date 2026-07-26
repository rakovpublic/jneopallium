/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;

/**
 * Kind of user intent decoded from neural activity.
 */
public enum IntentKind {
    REACH, GRASP, RELEASE, CURSOR_MOVE, CURSOR_CLICK, SPEECH_PHONEME, WHEELCHAIR_DRIVE, NONE
}
