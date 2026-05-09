/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** Mapper unit tests covering 01-PLC4X.md §4 quality table and §5 severity-map decode. */
class Plc4xSignalMapperTest {

    @Test
    void qualityMappingFollowsSpec() {
        assertEquals(Quality.GOOD,      Plc4xSignalMapper.toQuality(Plc4xResponseCode.OK));
        assertEquals(Quality.UNCERTAIN, Plc4xSignalMapper.toQuality(Plc4xResponseCode.NOT_FOUND));
        assertEquals(Quality.UNCERTAIN, Plc4xSignalMapper.toQuality(Plc4xResponseCode.ACCESS_DENIED));
        assertEquals(Quality.BAD,       Plc4xSignalMapper.toQuality(Plc4xResponseCode.INVALID_ADDRESS));
        assertEquals(Quality.BAD,       Plc4xSignalMapper.toQuality(Plc4xResponseCode.INVALID_DATATYPE));
        assertEquals(Quality.BAD,       Plc4xSignalMapper.toQuality(Plc4xResponseCode.INVALID_DATA));
        assertEquals(Quality.BAD,       Plc4xSignalMapper.toQuality(Plc4xResponseCode.INTERNAL_ERROR));
        assertEquals(Quality.BAD,       Plc4xSignalMapper.toQuality(Plc4xResponseCode.REMOTE_ERROR));
        assertEquals(Quality.BAD,       Plc4xSignalMapper.toQuality(null));
    }

    @Test
    void coerceHandlesNumericBooleanAndString() {
        assertEquals(1.0, Plc4xSignalMapper.coerceToDouble(Boolean.TRUE));
        assertEquals(0.0, Plc4xSignalMapper.coerceToDouble(Boolean.FALSE));
        assertEquals(42.5, Plc4xSignalMapper.coerceToDouble(42.5));
        assertEquals(7.0, Plc4xSignalMapper.coerceToDouble(7));
        assertEquals(3.14, Plc4xSignalMapper.coerceToDouble("3.14"), 1e-9);
        assertTrue(Double.isNaN(Plc4xSignalMapper.coerceToDouble(null)));
        assertTrue(Double.isNaN(Plc4xSignalMapper.coerceToDouble("hello")));
    }

    @Test
    void measurementSignalCarriesValueQualityAndTag() {
        Plc4xConfig.ReadBindingConfig cfg = new Plc4xConfig.ReadBindingConfig(
                "TIC-101", "S7", "%DB1.DBD0:REAL", "PLANT.TIC101.PV", 250L);
        MeasurementSignal ok = Plc4xSignalMapper.toMeasurement(
                cfg, Plc4xDriver.ReadResponse.ok(72.5), "plc4x-meas", 1_000L);
        assertEquals("PLANT.TIC101.PV", ok.getTag());
        assertEquals(72.5, ok.getMeasurement(), 1e-9);
        assertEquals(Quality.GOOD, ok.getQuality());
        assertEquals(1_000L, ok.getTimestamp());

        MeasurementSignal bad = Plc4xSignalMapper.toMeasurement(
                cfg, Plc4xDriver.ReadResponse.failure(Plc4xResponseCode.INVALID_ADDRESS),
                "plc4x-meas", 2_000L);
        assertEquals(Quality.BAD, bad.getQuality());
        assertTrue(Double.isNaN(bad.getMeasurement()),
                "non-OK reads should propagate NaN per 00-FRAMEWORK §0.5");
    }

    @Test
    void alarmWordDecodesEachMatchingBit() {
        Map<String, String> sev = new LinkedHashMap<>();
        sev.put("0x0001", "LOW");
        sev.put("0x0002", "HIGH");
        sev.put("0x0010", "CRITICAL");
        Plc4xConfig.EventBindingConfig cfg = new Plc4xConfig.EventBindingConfig(
                "TROUBLE", "S7", "%DB100.DBW0:WORD", "PLANT.LINE_A.TROUBLE", 1000L, sev);

        // value 0x0011 = bits 0x0001 (LOW) and 0x0010 (CRITICAL) set
        List<AlarmSignal> alarms = Plc4xSignalMapper.decodeAlarmWord(
                cfg, Plc4xDriver.ReadResponse.ok(0x0011), "plc4x-evt", 5_000L);
        assertEquals(2, alarms.size());
        assertEquals(AlarmPriority.LOW,    alarms.get(0).getPriority());
        assertEquals(AlarmPriority.URGENT, alarms.get(1).getPriority(),
                "CRITICAL should map to URGENT priority");
        assertEquals(5_000L, alarms.get(0).getTimestamp());
    }

    @Test
    void noAlarmEmittedWhenWordIsZero() {
        Map<String, String> sev = Map.of("0x0001", "LOW");
        Plc4xConfig.EventBindingConfig cfg = new Plc4xConfig.EventBindingConfig(
                "T", "S7", "%DB100.DBW0:WORD", "X", 1000L, sev);
        assertTrue(Plc4xSignalMapper.decodeAlarmWord(
                cfg, Plc4xDriver.ReadResponse.ok(0), "n", 1L).isEmpty());
    }

    @Test
    void parseMaskAcceptsHexAndDecimal() {
        assertEquals(1L, Plc4xSignalMapper.parseMask("0x0001"));
        assertEquals(16L, Plc4xSignalMapper.parseMask("0x10"));
        assertEquals(255L, Plc4xSignalMapper.parseMask("255"));
    }
}
