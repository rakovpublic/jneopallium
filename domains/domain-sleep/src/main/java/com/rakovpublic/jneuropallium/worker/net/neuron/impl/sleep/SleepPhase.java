/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

/**
 * Coarse sleep-wake phase. WAKE is the normal operating state; NREM2 and
 * NREM3 are deepening stages of non-REM sleep (with sharp-wave ripples
 * predominating in NREM3); REM is rapid-eye-movement sleep where
 * dreaming (recombination) occurs (Diekelmann &amp; Born 2010).
 */
public enum SleepPhase {
    WAKE,
    NREM2,
    NREM3,
    REM
}
