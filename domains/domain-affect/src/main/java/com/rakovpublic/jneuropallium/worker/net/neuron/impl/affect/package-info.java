/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Affect subsystem — biologically-inspired valence/arousal modulation layer.
 *
 * <p>Biological analogue: limbic system (amygdala, anterior insula, cingulate)
 * plus ascending interoceptive pathways. Implements the circumplex model of
 * affect (Russell 1980) on top of the {@code INeuron} abstraction.
 *
 * <p>Key classes:
 * <ul>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AmygdalaValenceNeuron}
 *       — fast loop threat/reward tagging.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AnteriorInsulaNeuron}
 *       — interoceptive integration.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AffectIntegrationNeuron}
 *       — appraisal + interoception integration; emits AffectStateSignal.</li>
 *   <li>{@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect.AffectModulationNeuron}
 *       — broadcasts modulation to learning, harm, attention.</li>
 * </ul>
 *
 * <p>References:
 * <ul>
 *   <li>LeDoux, J. (1998). The Emotional Brain.</li>
 *   <li>Craig, A.D. (2009). How do you feel — now? Nat Rev Neurosci 10:59–70.</li>
 *   <li>Russell, J.A. (1980). A circumplex model of affect.
 *       J Pers Soc Psychol 39(6):1161–1178.</li>
 *   <li>Damasio, A. (1996). The somatic marker hypothesis.
 *       Philos Trans R Soc Lond B 351:1413–1420.</li>
 * </ul>
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;
