/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.control;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

/**
 * Neutral contract for a safety interlock evaluated by the bridge write
 * framework ({@code AbstractBridgeOutputAggregator}). A tripped interlock
 * drives its bound outputs to their fail-safe value and no veto applies.
 */
public interface IInterlockSignal extends IResultSignal<Void> {

    /** Identifier of the interlock (matched against binding loop ids). */
    String getInterlockId();

    /** Whether the interlock is currently tripped. */
    boolean isTripped();
}
