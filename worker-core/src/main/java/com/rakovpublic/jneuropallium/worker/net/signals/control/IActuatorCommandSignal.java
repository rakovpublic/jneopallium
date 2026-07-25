/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.signals.control;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;

/**
 * Neutral contract for an actuator command carried out by the bridge write
 * framework ({@code AbstractBridgeOutputAggregator}). Concrete domain signals
 * (e.g. the industrial {@code ActuatorCommandSignal}) implement it so the
 * bridge SPI does not depend on any specific domain module.
 */
public interface IActuatorCommandSignal extends IResultSignal<Void> {

    /** Bound output tag this command targets. */
    String getTag();

    /** Proposed target value before clamping / rate-limiting. */
    double getTargetValue();

    /** Whether the command requests execution (vs. advisory-only). */
    boolean isExecute();
}
