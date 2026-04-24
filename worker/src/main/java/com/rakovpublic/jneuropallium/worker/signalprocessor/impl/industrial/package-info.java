/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Stateless signal processors for the industrial process-control module.
 * Every processor's {@code getNeuronClass()} returns an {@code I<Neuron>}
 * interface from
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial};
 * concrete neurons are dependency-injected at network construction time.
 *
 * <p>Signal → processor coverage:
 * <ul>
 *   <li>MeasurementSignal → MeasurementValidationProcessor,
 *       MeasurementPIDProcessor, MeasurementOscillationProcessor,
 *       MeasurementInterlockProcessor</li>
 *   <li>SetpointSignal → SetpointPIDProcessor</li>
 *   <li>ActuatorCommandSignal → ActuatorSafetyGateProcessor,
 *       ActuatorDispatchProcessor</li>
 *   <li>AlarmSignal → AlarmAggregationProcessor</li>
 *   <li>InterlockSignal → InterlockModeProcessor</li>
 *   <li>DegradationSignal → DegradationSchedulingProcessor</li>
 *   <li>EfficiencySignal → EfficiencyOptimiserProcessor</li>
 *   <li>BatchStateSignal → BatchModeProcessor</li>
 *   <li>OperatorOverrideSignal → OperatorOverrideProcessor</li>
 *   <li>MaintenanceWindowSignal → MaintenanceWindowSchedulingProcessor</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;
