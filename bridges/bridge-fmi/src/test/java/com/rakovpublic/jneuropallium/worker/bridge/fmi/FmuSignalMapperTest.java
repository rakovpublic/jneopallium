/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class FmuSignalMapperTest {

    private static final FmiBridgeConfig.ReadBindingConfig READ_CFG =
            new FmiBridgeConfig.ReadBindingConfig("TANK-TEMP", "tank.T", "PLANT.TANK01.TEMP");

    @Test
    void toMeasurementSetsFieldsCorrectly() {
        long ts = 123_456_789L;
        MeasurementSignal s = FmuSignalMapper.toMeasurement(READ_CFG, 55.3, "fmu-input", ts);

        assertEquals("PLANT.TANK01.TEMP", s.getTag());
        assertEquals(55.3, s.getMeasurement(), 1e-9);
        assertEquals(Quality.GOOD, s.getQuality());
        assertEquals(ts, s.getTimestamp());
        assertEquals("fmu-input", s.getInputName());
        assertEquals("TANK-TEMP", s.getName());
    }

    @Test
    void toAlarmReturnNullWhenInactive() {
        FmiBridgeConfig.EventBindingConfig cfg =
                new FmiBridgeConfig.EventBindingConfig("OVERTEMP", "alarm.over_temperature",
                        "PLANT.TANK01.OVERTEMP", "CRITICAL");
        AlarmSignal s = FmuSignalMapper.toAlarm(cfg, false, "fmu-event", 1L);
        assertNull(s, "inactive alarm should produce null");
    }

    @Test
    void toAlarmReturnSignalWhenActive() {
        FmiBridgeConfig.EventBindingConfig cfg =
                new FmiBridgeConfig.EventBindingConfig("OVERTEMP", "alarm.over_temperature",
                        "PLANT.TANK01.OVERTEMP", "CRITICAL");
        long ts = 42L;
        AlarmSignal s = FmuSignalMapper.toAlarm(cfg, true, "fmu-event", ts);
        assertNotNull(s);
        assertEquals("PLANT.TANK01.OVERTEMP", s.getTag());
        assertEquals(AlarmPriority.URGENT, s.getPriority());
        assertEquals("OVERTEMP", s.getConditionCode());
        assertEquals(ts, s.getTimestamp());
        assertEquals("fmu-event", s.getInputName());
    }

    @ParameterizedTest
    @CsvSource({
            "CRITICAL, URGENT",
            "URGENT,   URGENT",
            "HIGH,     HIGH",
            "LOW,      LOW",
            "JOURNAL,  JOURNAL",
            "UNKNOWN,  JOURNAL"
    })
    void severityMapsToCorrectAlarmPriority(String severity, AlarmPriority expected) {
        FmiBridgeConfig.EventBindingConfig cfg =
                new FmiBridgeConfig.EventBindingConfig("X", "x.alarm", "TAG", severity);
        AlarmSignal s = FmuSignalMapper.toAlarm(cfg, true, "n", 1L);
        assertNotNull(s);
        assertEquals(expected, s.getPriority());
    }
}
