/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Stateless signal processors for the industrial module. Every processor's
 * {@code getNeuronClass()} returns an {@code I<Neuron>} interface from
 * {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial};
 * concrete neurons are dependency-injected at network construction time.
 *
 * <p>Signal to processor coverage:
 * <ul>
 *   <li>MeasurementSignal to MeasurementValidationProcessor,
 *       MeasurementPIDProcessor, MeasurementOscillationProcessor,
 *       MeasurementInterlockProcessor, OperatingRegimeProcessor</li>
 *   <li>SetpointSignal to SetpointPIDProcessor</li>
 *   <li>ActuatorCommandSignal to ActuatorSafetyGateProcessor,
 *       ActuatorDispatchProcessor</li>
 *   <li>AlarmSignal to AlarmAggregationProcessor</li>
 *   <li>InterlockSignal to InterlockModeProcessor</li>
 *   <li>DegradationSignal to DegradationSchedulingProcessor</li>
 *   <li>EfficiencySignal to EfficiencyOptimiserProcessor</li>
 *   <li>BatchStateSignal to BatchModeProcessor</li>
 *   <li>OperatorOverrideSignal to OperatorOverrideProcessor</li>
 *   <li>MaintenanceWindowSignal to MaintenanceWindowSchedulingProcessor</li>
 *   <li>MachineWaveformSignal to AcousticFeatureProcessor,
 *       VibrationFeatureProcessor</li>
 *   <li>MachineFeatureSignal to MachineBaselineProcessor,
 *       FaultHypothesisProcessor</li>
 *   <li>DomainShiftSignal and OperatingRegimeSignal to context processors
 *       for IFaultHypothesisNeuron</li>
 *   <li>FaultHypothesisSignal to MachineHealthCorrelationProcessor</li>
 *   <li>MachineHealthAdvisorySignal to MachineHealthAdvisoryGateProcessor</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial;
