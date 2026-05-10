/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Lab Streaming Layer (LSL) bridge — Bridge 05 (05-LSL.md).
 *
 * <p>Wires the BCI / physiology research middleware
 * (<a href="https://labstreaminglayer.org/">labstreaminglayer.org</a>) into
 * the Jneopallium typed-signal pipeline:
 * <ul>
 *   <li>Inlets resolve LSL streams (EEG, ECoG, Spikes, EMG, HRV, GSR, Eye,
 *       Temperature, Markers) and emit
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.LFPSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.NeuralSpikeSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ECoGSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.InteroceptiveSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.affect.AppraisalSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.ThermalSignal} /
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.CalibrationSignal}.</li>
 *   <li>Outlets publish {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.StimulationCommandSignal}
 *       (only after the {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci.IStimulationSafetyGateNeuron}),
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.IntentSignal}, and
 *       {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.bci.SeizureRiskSignal}.</li>
 * </ul>
 *
 * <p>Bridge ceiling: <b>ADVISORY</b> (read-mostly). Stimulation outputs are
 * gated through the existing {@code StimulationSafetyGateNeuron}; the
 * outlets the bridge writes to are consumed by separately certified
 * stimulator-driver software.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;
