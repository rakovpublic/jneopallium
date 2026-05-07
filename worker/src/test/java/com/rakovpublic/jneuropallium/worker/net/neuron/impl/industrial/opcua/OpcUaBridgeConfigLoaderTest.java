/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.SafetyMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class OpcUaBridgeConfigLoaderTest {

    private static final String GOOD_YAML = """
            connection:
              endpointUrl: "opc.tcp://example:4840"
              applicationName: "Test-Bridge"
              applicationUri: "urn:test:bridge"
              requestTimeout: "PT5S"
              sessionTimeout: "PT2M"
              keepAliveFailuresAllowed: 3
            security:
              policy: NONE
              mode: NONE
              auth:
                type: "Anonymous"
            reads:
              - loopId: "TIC-101"
                nodeId: "ns=2;s=Plant.Temperature"
                signalTag: "PLANT.TIC101.PV"
                direction: READ
            writes:
              - loopId: "FIC-101"
                nodeId: "ns=2;s=Plant.TargetMotorSpeed"
                signalTag: "PLANT.FIC101.SP"
                direction: WRITE
                failSafeValue: 0.0
                rampRateMaxPerSec: 5.0
                minClampValue: 0.0
                maxClampValue: 100.0
            alarms:
              - loopId: "PLANT-ALARMS"
                nodeId: "ns=2;s=Plant.Alarm"
                signalTag: "PLANT.ALARM"
                direction: READ
            audit:
              localAuditFile: "/tmp/audit.jsonl"
              writeRejectedToAudit: true
            perLoopSafetyMode:
              TIC-101: SHADOW
              FIC-101: ADVISORY
            tickInterval: "PT0.25S"
            """;

    @Test
    void loadsExampleYaml() throws IOException {
        OpcUaBridgeConfig cfg = OpcUaBridgeConfigLoader.load(GOOD_YAML);
        assertEquals("opc.tcp://example:4840", cfg.connection().endpointUrl());
        assertEquals(Duration.ofSeconds(5), cfg.connection().requestTimeout());
        assertEquals(Duration.ofMinutes(2), cfg.connection().sessionTimeout());
        assertEquals(3, cfg.connection().keepAliveFailuresAllowed());
        assertEquals(1, cfg.reads().size());
        assertEquals("PLANT.TIC101.PV", cfg.reads().get(0).signalTag());
        assertEquals(1, cfg.writes().size());
        assertEquals(0.0, cfg.writes().get(0).failSafeValue());
        assertEquals(5.0, cfg.writes().get(0).rampRateMaxPerSec());
        assertEquals(0.0, cfg.writes().get(0).minClampValue());
        assertEquals(100.0, cfg.writes().get(0).maxClampValue());
        assertEquals(SafetyMode.SHADOW, cfg.perLoopSafetyMode().get("TIC-101"));
        assertEquals(SafetyMode.ADVISORY, cfg.perLoopSafetyMode().get("FIC-101"));
        assertEquals(Duration.ofMillis(250), cfg.tickInterval());
        assertInstanceOf(OpcUaBridgeConfig.SecurityConfig.Anonymous.class, cfg.security().auth());
    }

    @Test
    void unknownPropertyIsRejected() {
        // Add a wholly-unknown field at the top level so neither the typo
        // path (which would surface as a constructor failure on a record)
        // nor the unknown-field path is masked.
        String bad = GOOD_YAML.replace(
                "tickInterval: \"PT0.25S\"",
                "tickInterval: \"PT0.25S\"\nthisFieldDoesNotExist: 42");
        Throwable thrown = assertThrows(Throwable.class,
                () -> OpcUaBridgeConfigLoader.load(bad),
                "FAIL_ON_UNKNOWN_PROPERTIES must be true");
        assertTrue(thrown instanceof UnrecognizedPropertyException
                        || (thrown.getCause() instanceof UnrecognizedPropertyException),
                "expected UnrecognizedPropertyException, got " + thrown);
    }

    @Test
    void usernamePasswordAuthRoundTrips() throws IOException {
        String yaml = GOOD_YAML.replace(
                "auth:\n    type: \"Anonymous\"",
                "auth:\n    type: \"UsernamePassword\"\n    username: \"User\"\n    passwordEnv: \"OPCUA_USER_PASSWORD\"");
        OpcUaBridgeConfig cfg = OpcUaBridgeConfigLoader.load(yaml);
        assertInstanceOf(OpcUaBridgeConfig.SecurityConfig.UsernamePassword.class, cfg.security().auth());
        OpcUaBridgeConfig.SecurityConfig.UsernamePassword up =
                (OpcUaBridgeConfig.SecurityConfig.UsernamePassword) cfg.security().auth();
        assertEquals("User", up.username());
    }
}
