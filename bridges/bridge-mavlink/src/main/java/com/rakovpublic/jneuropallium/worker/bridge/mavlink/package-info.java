/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * MAVLink bridge (12-MAVLINK.md). Read-and-advisory-only adapter between a
 * MAVLink-speaking system (ArduPilot SITL, PX4 SITL, or — once the safety
 * ceiling is satisfied — a hardware autopilot) and the Jneopallium signal
 * pipeline.
 *
 * <p>Structural ceiling per 12-MAVLINK.md §3 is <b>SIM-ONLY</b>. Egress is
 * limited to advisory message types ({@code STATUSTEXT},
 * {@code NAMED_VALUE_FLOAT}, custom {@code JNEO_*} dialect); writes that
 * would arm, take off, or otherwise drive an autopilot are rejected at
 * config load and again at runtime unless {@code simulatorOnly: true}.
 */
package com.rakovpublic.jneuropallium.worker.bridge.mavlink;
