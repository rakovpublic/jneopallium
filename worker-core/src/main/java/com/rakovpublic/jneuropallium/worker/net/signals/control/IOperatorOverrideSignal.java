/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.control;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

/**
 * Neutral contract for an operator override registered by the bridge write
 * framework ({@code AbstractBridgeOutputAggregator}). While active for a tag,
 * automatic commands for that tag are held.
 */
public interface IOperatorOverrideSignal extends IResultSignal<Void> {

    /** Output tag the override applies to. */
    String getTag();

    /** Operator who issued the override. */
    String getOperatorId();

    /** Human-readable reason for the override. */
    String getReason();

    /** Manual value the operator holds the output at. */
    double getManualValue();
}
