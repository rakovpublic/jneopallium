/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig.NodeBindingConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig.NodeBindingConfig.Direction;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Canonical wiring constants and config factory for demo-01, matching
 * {@code /tmp/demo01-reactor.yaml} in {@code demo-01-reactor-cascade-control.md}.
 *
 * <p>Building the config in code (rather than only from YAML) lets the
 * acceptance test perform the documented SHADOW → AUTONOMOUS "restart"
 * (config is deliberately not hot-reloaded) by constructing a fresh config
 * with a different {@code perLoopSafetyMode}.
 */
public final class Demo01Config {

    private Demo01Config() {}

    public static final String TEMP_TAG = "PLANT.TIC101.PV";
    public static final String FLOW_TAG = "PLANT.FIC101.PV";
    public static final String VALVE_TAG = "PLANT.FIC101.OUT";
    public static final String INNER_SP_TAG = "PLANT.FIC101.SP";
    public static final String ALARM_TAG = "PLANT.TIC101.HI_ILK";

    public static final String VALVE_LOOP_ID = "FIC-101";
    public static final double TEMP_SETPOINT = 80.0;
    public static final double INTERLOCK_THRESHOLD = 110.0;
    public static final double FAIL_SAFE_VALVE = 100.0;
    public static final double RAMP_RATE_MAX_PER_SEC = 25.0;
    public static final Duration TICK = Duration.ofMillis(100);

    public static OpcUaBridgeConfig build(String auditFile, SafetyMode valveMode) {
        var connection = new OpcUaBridgeConfig.ConnectionConfig(
                "opc.tcp://localhost:4840/jneopallium/reactor",
                "Jneopallium-Reactor-Demo",
                "urn:rakovpublic:jneopallium:demo01",
                Duration.ofSeconds(5), Duration.ofMinutes(2), 3);

        var security = new OpcUaBridgeConfig.SecurityConfig(
                OpcUaBridgeConfig.SecurityConfig.SecurityPolicy.NONE,
                OpcUaBridgeConfig.SecurityConfig.MessageSecurityMode.NONE,
                null, null, null,
                new OpcUaBridgeConfig.SecurityConfig.Anonymous());

        var ticRead = new NodeBindingConfig(
                "TIC-101", "ns=2;s=Reactor.TIC101.PV", TEMP_TAG, Direction.READ,
                null, null, null, null);
        var ficRead = new NodeBindingConfig(
                "FIC-101", "ns=2;s=Reactor.FIC101.PV", FLOW_TAG, Direction.READ,
                null, null, null, null);

        var valveWrite = new NodeBindingConfig(
                VALVE_LOOP_ID, "ns=2;s=Reactor.FIC101.OUT", VALVE_TAG, Direction.WRITE,
                FAIL_SAFE_VALVE, RAMP_RATE_MAX_PER_SEC, 0.0, 100.0);

        var alarm = new NodeBindingConfig(
                "HI_TEMP_ILK", "ns=2;s=Reactor.HiTempInterlock", ALARM_TAG, Direction.READ,
                null, null, null, null);

        var audit = new OpcUaBridgeConfig.AuditConfig(auditFile, null, true);

        return new OpcUaBridgeConfig(
                connection, security,
                List.of(ticRead, ficRead),
                List.of(valveWrite),
                List.of(alarm),
                audit,
                Map.of(VALVE_LOOP_ID, valveMode),
                TICK);
    }
}
