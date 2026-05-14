/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * Nengo peer-runtime integration (15-NENGO.md).
 *
 * <p>Unlike the protocol bridges under {@code worker.bridge.X}, the Nengo
 * counterpart is itself a cognitive runtime. Jneopallium remains the master
 * runtime (planning, safety, audit); Nengo acts as a spiking-neural-network
 * encoder on the input side and a smooth-vector realizer on the output side.
 *
 * <p>Transport is newline-delimited JSON (JSONL) over a Unix domain socket
 * (live demos) or an append-only file (deterministic CI replay). Frame
 * schema is shared with the Python counterpart in
 * {@code worker/src/test/python/nengo/}.
 *
 * <p>The integration enforces the same §0 ground rules and audit schema as
 * the protocol bridges: safe-by-default {@code SHADOW}, explicit per-tag
 * promotion to {@code ADVISORY}, {@code AUTONOMOUS} only allowed when
 * {@code simulatorOnly: true}.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;
