/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * HL7 FHIR bridge — Bridge 06 (06-FHIR.md).
 *
 * <p>Wires Jneopallium's clinical signal pipeline into a FHIR (R4 / R5)
 * REST endpoint as a <b>permanently advisory</b> consumer:
 *
 * <ul>
 *   <li>Polls configured FHIR searches against a cohort of patient ids
 *       and emits typed clinical signals
 *       ({@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.WaveformSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DiagnosisHypothesisSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal}).</li>
 *   <li>Emits {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.TreatmentProposalSignal},
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal}, and
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal}
 *       <b>only</b> to the local JSONL audit and the configured advisory
 *       file — never as FHIR write operations.</li>
 * </ul>
 *
 * <p><b>Bridge ceiling: ADVISORY — permanently.</b> This package contains
 * no FHIR write code path. The {@link FhirTransport} interface defines
 * the entire FHIR surface the bridge can use, and that interface
 * deliberately exposes only {@code read()} / {@code search()} (06-FHIR.md
 * §3 rule 1). Adding a {@code create} / {@code update} / {@code delete}
 * operation later would be a breaking change to the public seam.
 *
 * <p>Production wiring constructs a
 * {@link JdkHttpFhirTransport} (default) or a HAPI-backed transport
 * (provided by an external module) and hands it to
 * {@link FhirClientService}. Acceptance tests use the
 * {@link InMemoryFhirTransport}.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;
