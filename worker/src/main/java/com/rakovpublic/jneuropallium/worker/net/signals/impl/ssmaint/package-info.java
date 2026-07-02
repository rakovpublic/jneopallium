/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Typed signals for the self-supervised maintenance module.
 *
 * <p>Flow: {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.AssetTelemetrySignal}
 * (raw multi-sensor frame, fast loop) is reconstructed into a
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ReconResidualSignal},
 * fused into a
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.HealthHypothesisSignal},
 * and gated into a read-only
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.MaintenanceAdvisorySignal}.
 * The continuous-learning path carries an
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.OperatorFeedbackSignal}
 * into the adapter, which emits a
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal}
 * that the gate applies live.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint;
