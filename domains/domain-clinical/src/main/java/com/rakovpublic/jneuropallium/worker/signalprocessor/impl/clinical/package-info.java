/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Stateless signal processors for the clinical decision-support module.
 * Per the framework rule "stateless processors, stateful neurons", each
 * processor holds no mutable state and is parameterised by an
 * {@code I<Neuron>} interface — concrete neuron implementations are
 * dependency-injected at network-construction time so processors remain
 * decoupled from any particular implementation.
 *
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.VitalMonitorProcessor}
 *       — guardrail check, may emit {@code AdverseEventAlertSignal}.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.VitalTrendProcessor}
 *       — feeds the per-vital trend detector.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.VitalAcuityProcessor}
 *       — updates NEWS2-style acuity score.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.WaveformAnalysisProcessor}
 *       — ECG/PPG/EEG classification, may emit alerts.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.DemographicContextProcessor}
 *       — patient context refresh.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.MedicationRegimenProcessor}
 *       — keeps the active-drug list in sync for DDI lookups.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.LabEvidenceProcessor}
 *       — Bayesian update from abnormal lab.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.ImagingEvidenceProcessor}
 *       — Bayesian update from imaging.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical.ContraindicationProcessor}
 *       — runs every proposed treatment through the contraindication
 *         filter and forwards any {@code ClinicalVetoSignal}.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.clinical;
