/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Sleep / replay / offline-consolidation subsystem.
 *
 * <p>Biological analogues and citations:
 * <ul>
 *   <li>Hippocampal replay (NREM): Wilson &amp; McNaughton 1994;
 *       Foster &amp; Wilson 2006.</li>
 *   <li>Sharp-wave ripples: Buzsáki 2015.</li>
 *   <li>REM dreaming / recombination: Wamsley &amp; Stickgold 2011.</li>
 *   <li>Sleep-dependent memory consolidation: Diekelmann &amp; Born 2010.</li>
 * </ul>
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.SleepControllerNeuron}
 *       — orchestrates sleep phases.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.HippocampalReplayNeuron}
 *       — top-K salience-ordered episode replay.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.SharpWaveRippleNeuron}
 *       — NREM3 burst compression targeting LTM.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep.REMDreamingNeuron}
 *       — REM recombination for hypothetical planning.</li>
 * </ul>
 *
 * <p>Safety invariant: REM-generated plans are forwarded only as
 * candidates; the harm discriminator still gates motor execution.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;
