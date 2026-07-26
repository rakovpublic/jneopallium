/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrial;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfigLoader;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Proves the documented demo-01 YAML parses under the bridge loader's strict
 * {@code FAIL_ON_UNKNOWN_PROPERTIES} contract and agrees with the in-code
 * {@link Demo01Config} used by the runner and acceptance test.
 */
class Demo01ConfigYamlTest {

    @Test
    void documentedYaml_loadsAndMatchesInCodeConfig() throws Exception {
        OpcUaBridgeConfig yaml;
        try (InputStream in = getClass().getResourceAsStream("/demo/demo01-reactor.yaml")) {
            assertNotNull(in, "demo01-reactor.yaml must be on the test classpath");
            yaml = OpcUaBridgeConfigLoader.load(in);
        }

        assertEquals("opc.tcp://localhost:4840/jneopallium/reactor", yaml.connection().endpointUrl());
        assertEquals(Duration.ofMillis(100), yaml.tickInterval());
        assertEquals(SafetyMode.SHADOW, yaml.perLoopSafetyMode().get(Demo01Config.VALVE_LOOP_ID));
        assertEquals(2, yaml.reads().size());
        assertEquals(1, yaml.writes().size());

        var valve = yaml.writes().get(0);
        assertEquals(Demo01Config.VALVE_TAG, valve.signalTag());
        assertEquals(Demo01Config.FAIL_SAFE_VALVE, valve.failSafeValue());
        assertEquals(Demo01Config.RAMP_RATE_MAX_PER_SEC, valve.rampRateMaxPerSec());
        assertEquals(0.0, valve.minClampValue());
        assertEquals(100.0, valve.maxClampValue());

        // The in-code factory mirrors the YAML (same bindings, clamps, tick).
        OpcUaBridgeConfig coded = Demo01Config.build("/tmp/jneopallium-demo01-audit.jsonl", SafetyMode.SHADOW);
        assertEquals(yaml.connection().endpointUrl(), coded.connection().endpointUrl());
        assertEquals(yaml.tickInterval(), coded.tickInterval());
        assertEquals(yaml.writes().get(0).signalTag(), coded.writes().get(0).signalTag());
        assertEquals(yaml.writes().get(0).failSafeValue(), coded.writes().get(0).failSafeValue());
        assertEquals(yaml.alarms().get(0).signalTag(), coded.alarms().get(0).signalTag());
    }
}
