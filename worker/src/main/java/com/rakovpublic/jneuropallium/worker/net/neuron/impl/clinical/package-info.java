/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Clinical Decision Support &amp; Differential Diagnosis module for the
 * jneopallium autonomous-AI framework.
 *
 * <p>The module maps medical reasoning's five timescales (seconds → years)
 * onto the framework's {@code ProcessingFrequency(loop, epoch)} signals and
 * pipes differential diagnosis, treatment planning, drug-interaction
 * checking, and contraindication filtering through the existing
 * harm-discriminator safety path. The recommendation neuron never emits
 * an executable action; clinician confirmation is a hard gate.
 *
 * <h2>Regulatory posture</h2>
 * Aligns with FDA 510(k) Class II decision-support software; software
 * lifecycle per IEC&nbsp;62304:2006+A1:2015; risk management per
 * ISO&nbsp;14971:2019. Autonomy claims are out of scope (would require
 * Class III).
 *
 * <h2>Policy</h2>
 * The affect, curiosity, and sleep modules must remain disabled in
 * clinical mode — they introduce between-patient variability that is
 * inappropriate for a medical device.
 *
 * @see com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical;
