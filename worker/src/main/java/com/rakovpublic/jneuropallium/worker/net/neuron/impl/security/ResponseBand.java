/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

/** Graduated-response band from spec §6. */
public enum ResponseBand { LOG, ALERT, RATE_LIMIT, CONNECTION_QUARANTINE, HOST_QUARANTINE, ESCALATE }
