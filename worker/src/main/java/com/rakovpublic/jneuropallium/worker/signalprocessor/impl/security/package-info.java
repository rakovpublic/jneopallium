/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Stateless signal processors for the cybersecurity / immune module.
 * Each processor is parameterised by an {@code I<Neuron>} interface
 * from
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.security};
 * concrete neuron implementations are dependency-injected at
 * network-construction time.
 *
 * <p>Every signal type under
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.security}
 * has at least one processor:
 * <ul>
 *   <li>PacketSignal → PacketSignatureProcessor, PacketFlowProcessor</li>
 *   <li>SyscallSignal → SyscallBehaviourProcessor</li>
 *   <li>LogEventSignal → LogSignatureProcessor</li>
 *   <li>SignatureMatchSignal → SignatureToleranceProcessor,
 *       SignatureHypothesisProcessor</li>
 *   <li>AnomalyScoreSignal → AnomalyHypothesisProcessor</li>
 *   <li>ThreatHypothesisSignal → HypothesisResponseProcessor</li>
 *   <li>QuarantineRequestSignal → QuarantineGateProcessor,
 *       QuarantineApplyProcessor</li>
 *   <li>QuarantineLiftSignal → QuarantineLiftProcessor</li>
 *   <li>InflammationBroadcastSignal → InflammationBaselineProcessor</li>
 *   <li>SelfToleranceSignal → SelfToleranceProcessor</li>
 *   <li>IncidentReportSignal → IncidentFatigueProcessor,
 *       IncidentRollbackProcessor</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security;
