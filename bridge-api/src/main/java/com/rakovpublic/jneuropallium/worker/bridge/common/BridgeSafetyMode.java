/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

/**
 * Per-loop deployment mode (00-FRAMEWORK §7).
 *
 * <p>Mirrors the industrial {@code SafetyMode} so that non-industrial bridges
 * (FHIR, Kafka, OTel, …) can use the framework without depending on the
 * industrial package.
 */
public enum BridgeSafetyMode { SHADOW, ADVISORY, AUTONOMOUS }
