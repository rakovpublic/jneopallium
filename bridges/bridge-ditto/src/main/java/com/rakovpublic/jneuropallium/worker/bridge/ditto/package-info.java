/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Eclipse Ditto digital-twin bridge (10-DITTO.md). A Ditto {@code Thing} is
 * the upstream device-fleet model: features carry numeric / boolean state,
 * a policy gates access. The bridge subscribes to twin events, emits typed
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal}
 * /
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal}
 * instances per the ingress mapping in 10-DITTO §4, and writes neuron-derived
 * advisory setpoints back as twin commands targeting {@code recommended_*} /
 * {@code advisory_*} features only — never the actual control feature.
 *
 * <p>The structural ceiling is {@code ADVISORY} (10-DITTO §1, README index).
 * The {@link com.rakovpublic.jneuropallium.worker.bridge.ditto.DittoBridgeConfig}
 * compact constructor rejects any per-tag {@code AUTONOMOUS} promotion, and
 * the {@link com.rakovpublic.jneuropallium.worker.bridge.ditto.DittoBridgeConfigLoader}
 * rejects any write binding whose feature name lacks the
 * {@code recommended_} / {@code advisory_} prefix (§4 egress table).
 *
 * @see <a href="../../../../../../../../../../10-DITTO.md">10-DITTO.md</a>
 * @see <a href="../../../../../../../../../../00-FRAMEWORK.md">00-FRAMEWORK.md</a>
 */
package com.rakovpublic.jneuropallium.worker.bridge.ditto;
