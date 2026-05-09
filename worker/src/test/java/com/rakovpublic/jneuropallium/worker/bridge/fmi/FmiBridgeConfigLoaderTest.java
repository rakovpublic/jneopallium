/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class FmiBridgeConfigLoaderTest {

    @Test
    void loadsTankTemperatureConfig() throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fmi/tank_temperature.yaml")) {
            assertNotNull(in, "test resource not found");
            FmiBridgeConfig cfg = FmiBridgeConfigLoader.load(in);

            // FMU section
            assertEquals("./src/test/resources/fmu/tank_temperature.fmu", cfg.fmu().path());
            assertFalse(cfg.fmu().loggingOn());
            assertTrue(cfg.fmu().toleranceDefined());
            assertEquals(1e-6, cfg.fmu().tolerance(), 1e-12);

            // Clock
            assertEquals(FmiBridgeConfig.ClockConfig.ClockMode.AS_FAST_AS_POSSIBLE, cfg.clock().mode());
            assertEquals(0.0, cfg.clock().startTime());
            assertEquals(0.25, cfg.clock().stepSize(), 1e-9);

            // Reads
            assertEquals(1, cfg.reads().size());
            FmiBridgeConfig.ReadBindingConfig r = cfg.reads().get(0);
            assertEquals("TANK-TEMP", r.bindingId());
            assertEquals("tank.T", r.fmuVariable());
            assertEquals("PLANT.TANK01.TEMP", r.signalTag());

            // Writes
            assertEquals(1, cfg.writes().size());
            FmiBridgeConfig.WriteBindingConfig w = cfg.writes().get(0);
            assertEquals("HEATER-Q", w.bindingId());
            assertEquals("heater.Q", w.fmuVariable());
            assertEquals("PLANT.HEATER01.SP", w.signalTag());
            assertEquals(0.0, w.failSafeValue(), 1e-9);
            assertEquals(0.0, w.minClampValue(), 1e-9);
            assertEquals(50000.0, w.maxClampValue(), 1e-9);

            // Events
            assertEquals(1, cfg.events().size());
            FmiBridgeConfig.EventBindingConfig e = cfg.events().get(0);
            assertEquals("OVERTEMP", e.bindingId());
            assertEquals("alarm.over_temperature", e.fmuVariable());
            assertEquals("CRITICAL", e.severity());

            // Audit
            assertNotNull(cfg.audit());
            assertTrue(cfg.audit().writeRejectedToAudit());

            // Safety modes
            assertEquals(BridgeSafetyMode.AUTONOMOUS, cfg.perTagSafetyMode().get("HEATER-Q"));
        }
    }

    @Test
    void loadsFromYamlString() throws Exception {
        String yaml = """
                fmu:
                  path: /tmp/test.fmu
                  loggingOn: true
                  toleranceDefined: false
                  tolerance: 0.0
                clock:
                  mode: REAL_TIME
                  startTime: 1.0
                  stepSize: 0.1
                audit:
                  localAuditFile: /tmp/audit.jsonl
                  writeRejectedToAudit: false
                """;
        FmiBridgeConfig cfg = FmiBridgeConfigLoader.load(yaml);
        assertEquals("/tmp/test.fmu", cfg.fmu().path());
        assertTrue(cfg.fmu().loggingOn());
        assertEquals(FmiBridgeConfig.ClockConfig.ClockMode.REAL_TIME, cfg.clock().mode());
        assertEquals(1.0, cfg.clock().startTime());
        assertEquals(0.1, cfg.clock().stepSize(), 1e-9);
        assertTrue(cfg.reads().isEmpty());
        assertTrue(cfg.writes().isEmpty());
        assertTrue(cfg.events().isEmpty());
    }

    @Test
    void defaultClockModeIsAfap() throws Exception {
        String yaml = """
                fmu:
                  path: /tmp/x.fmu
                  loggingOn: false
                  toleranceDefined: false
                  tolerance: 0.0
                clock:
                  startTime: 0.0
                  stepSize: 0.5
                audit:
                  localAuditFile: /tmp/a.jsonl
                  writeRejectedToAudit: false
                """;
        FmiBridgeConfig cfg = FmiBridgeConfigLoader.load(yaml);
        assertEquals(FmiBridgeConfig.ClockConfig.ClockMode.AS_FAST_AS_POSSIBLE, cfg.clock().mode());
    }
}
