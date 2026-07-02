/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Self-supervised maintenance module: label-free predictive maintenance with
 * continuous online learning from operator feedback.
 *
 * <p>The model is trained without any fault labels. Each
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.CrossSensorReconstructionNeuron}
 * predicts a sensor from its peers (regime-standardised) and reports the
 * residual; the
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.MaintenanceHypothesisNeuron}
 * fuses residuals into a trend / change-point / severity / evidence hypothesis
 * with a lead time. The
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.FeedbackAdaptationNeuron}
 * adapts per-family thresholds from operator feedback (bounded, rate-limited,
 * frozen during domain shift), and the
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.SsAdvisoryGateNeuron}
 * applies those live thresholds to emit read-only advisories.
 *
 * <p>Architectural invariants:
 * <ul>
 *   <li>No labels: reconstruction targets are other sensors; severity is
 *       calibrated from the asset's own healthy-window percentiles.</li>
 *   <li>Learning is continuous and in-place: threshold updates flow as
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint.ThresholdUpdateSignal}
 *       into the running gate — no redeploy.</li>
 *   <li>Advisory-only:
 *       {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint.SsMaintConfig}
 *       forbids disabling the advisory posture; the model never actuates.</li>
 * </ul>
 *
 * <p>Initial fitting and tests live in
 * {@code scripts/demo-self-supervised-maintenance} (Python, no third-party
 * dependencies); the fitted parameters are carried in the deployable layer
 * configuration under {@code model/self-supervised-maintenance}.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.signals.impl.ssmaint
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.ssmaint;
