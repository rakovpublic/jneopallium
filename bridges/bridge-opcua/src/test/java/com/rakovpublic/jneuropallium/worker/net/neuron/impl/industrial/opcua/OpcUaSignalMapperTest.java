/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class OpcUaSignalMapperTest {

    private static OpcUaNodeBinding binding(String tag) {
        return new OpcUaNodeBinding(new OpcUaBridgeConfig.NodeBindingConfig(
                "LOOP-1", "ns=2;s=" + tag, tag,
                OpcUaBridgeConfig.NodeBindingConfig.Direction.READ,
                null, null, null, null));
    }

    @Test
    void measurementWithGoodStatusCarriesGoodQuality() {
        DataValue dv = new DataValue(
                Variant.ofDouble(42.5),
                StatusCode.GOOD,
                new DateTime(Instant.ofEpochMilli(1_700_000_000_000L)));
        MeasurementSignal s = OpcUaSignalMapper.toMeasurement(binding("T1"), dv);
        assertEquals(42.5, s.getMeasurement(), 1e-9);
        assertEquals(Quality.GOOD, s.getQuality());
        assertEquals(1_700_000_000_000L, s.getTimestamp());
        assertEquals("T1", s.getTag());
    }

    @Test
    void badStatusYieldsBadQuality() {
        DataValue dv = new DataValue(Variant.ofDouble(0.0), StatusCode.BAD);
        MeasurementSignal s = OpcUaSignalMapper.toMeasurement(binding("T1"), dv);
        assertEquals(Quality.BAD, s.getQuality(), "BAD must propagate, never become GOOD");
    }

    @Test
    void uncertainStatusYieldsUncertainQuality() {
        DataValue dv = new DataValue(Variant.ofDouble(0.0), StatusCode.UNCERTAIN);
        MeasurementSignal s = OpcUaSignalMapper.toMeasurement(binding("T1"), dv);
        assertEquals(Quality.UNCERTAIN, s.getQuality());
    }

    @Test
    void timestampFallsBackToServerThenLocal() {
        long before = System.currentTimeMillis();
        DataValue dvServer = new DataValue(
                Variant.ofDouble(1.0),
                StatusCode.GOOD,
                null,
                new DateTime(Instant.ofEpochMilli(1_700_000_000_000L)));
        MeasurementSignal s1 = OpcUaSignalMapper.toMeasurement(binding("T"), dvServer);
        assertEquals(1_700_000_000_000L, s1.getTimestamp());

        DataValue dvNone = new DataValue(Variant.ofDouble(1.0), StatusCode.GOOD, null, null);
        MeasurementSignal s2 = OpcUaSignalMapper.toMeasurement(binding("T"), dvNone);
        assertTrue(s2.getTimestamp() >= before);
    }

    @Test
    void alarmPriorityMapsByCode() {
        assertEquals(AlarmPriority.JOURNAL, OpcUaSignalMapper.mapAlarmPriority(50));
        assertEquals(AlarmPriority.LOW, OpcUaSignalMapper.mapAlarmPriority(150));
        assertEquals(AlarmPriority.HIGH, OpcUaSignalMapper.mapAlarmPriority(500));
        assertEquals(AlarmPriority.URGENT, OpcUaSignalMapper.mapAlarmPriority(900));
    }

    @Test
    void alarmSignalCarriesConditionCode() {
        DataValue dv = new DataValue(
                Variant.ofInt32(800),
                StatusCode.GOOD,
                new DateTime(1L));
        AlarmSignal s = OpcUaSignalMapper.toAlarm(binding("PLANT.ALARM"), dv);
        assertEquals(AlarmPriority.URGENT, s.getPriority());
        assertEquals("OPCUA-800", s.getConditionCode());
    }

    @Test
    void coerceDoubleAcceptsBoolean() {
        DataValue dv = new DataValue(Variant.ofBoolean(true), StatusCode.GOOD);
        MeasurementSignal s = OpcUaSignalMapper.toMeasurement(binding("X"), dv);
        assertEquals(1.0, s.getMeasurement(), 0.0);
    }

    @Test
    void nullVariantBecomesNaN() {
        DataValue dv = new DataValue(Variant.NULL_VALUE, StatusCode.GOOD);
        MeasurementSignal s = OpcUaSignalMapper.toMeasurement(binding("X"), dv);
        assertTrue(Double.isNaN(s.getMeasurement()));
    }
}
