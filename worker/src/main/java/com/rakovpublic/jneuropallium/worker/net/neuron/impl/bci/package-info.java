/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * BCI / neural-prosthetics module. Implements the closed-loop pipeline
 * described in {@code use-case-bci-neural-prosthetics.md}: raw neural
 * acquisition (spikes / LFP / ECoG) → spike sorting and firing-rate
 * estimation → intent decoding (Georgopoulos population vector, Kalman,
 * LFADS-style latent dynamics, speech phonemes) → user-state and intent
 * fusion → prosthetic planning → safety-gated stimulation and actuation.
 * Hard safety invariants enforced at Layer 5 (Shannon charge-density
 * criterion, charge balance, seizure lockout) and Layer 7 (thermal
 * shutdown, power budget).
 *
 * <p>Biological and clinical references:</p>
 * <ul>
 *   <li>Georgopoulos, A.P. et al. (1986). Neuronal population coding of
 *       movement direction. <i>Science</i> 233, 1416–1419.</li>
 *   <li>Wu, W. et al. (2006). Bayesian population decoding of motor cortical
 *       activity using a Kalman filter. <i>Neural Computation</i> 18.</li>
 *   <li>Velliste, M. et al. (2008). Cortical control of a prosthetic arm for
 *       self-feeding. <i>Nature</i> 453, 1098–1101.</li>
 *   <li>Hochberg, L.R. et al. (2012). Reach and grasp by people with
 *       tetraplegia using a neurally controlled robotic arm. <i>Nature</i>
 *       485, 372–375.</li>
 *   <li>Pandarinath, C. et al. (2018). Inferring single-trial neural
 *       population dynamics using sequential auto-encoders (LFADS).
 *       <i>Nature Methods</i> 15, 805–815.</li>
 *   <li>Flesher, S.N. et al. (2021). A brain-computer interface that evokes
 *       tactile sensations improves robotic arm control. <i>Science</i>
 *       372, 831–836.</li>
 *   <li>Shannon, R.V. (1992). A model of safe levels for electrical
 *       stimulation. <i>IEEE Trans Biomed Eng</i> 39, 424–426.</li>
 *   <li>Cogan, S.F. (2008). Neural stimulation and recording electrodes.
 *       <i>Annu Rev Biomed Eng</i> 10, 275–309.</li>
 *   <li>Worrell, G.A. et al. (2008). High-frequency oscillations in human
 *       temporal lobe: simultaneous microwire and clinical macroelectrode
 *       recordings. <i>Brain</i> 131, 928–937.</li>
 *   <li>Wolpert, D.M., Miall, R.C., Kawato, M. (1998). Internal models in
 *       the cerebellum. <i>Trends Cogn Sci</i> 2, 338–347.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci;
