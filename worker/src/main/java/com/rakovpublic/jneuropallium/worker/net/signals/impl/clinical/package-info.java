/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Clinical-domain signal types for the jneopallium decision-support module.
 * All signals declare an explicit {@code ProcessingFrequency(loop, epoch)}
 * static field so that the network scheduler places each signal on the
 * correct biological timescale — vitals on loop 1, labs on loop 2/epoch 3,
 * imaging on loop 2/epoch 5, demographics on loop 2/epoch 10.
 *
 * <p>{@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ClinicalVetoSignal}
 * deliberately specialises the existing {@code HarmVetoSignal} from the
 * harm-discriminator module so that every safety veto, clinical or not,
 * flows through a single audit path.
 *
 * <h2>Coding systems</h2>
 * <ul>
 *   <li>Lab analytes: LOINC</li>
 *   <li>Diagnoses: ICD-10</li>
 *   <li>Medications: RxNorm</li>
 *   <li>Anatomy: BodyPart codes (per spec)</li>
 * </ul>
 *
 * @see com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical
 */
package com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical;
