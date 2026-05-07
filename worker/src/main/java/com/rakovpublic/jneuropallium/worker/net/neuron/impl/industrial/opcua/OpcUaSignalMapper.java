/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.DateTime;
import org.eclipse.milo.opcua.stack.core.types.builtin.StatusCode;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;

/**
 * Pure-function translation between OPC UA {@link DataValue}s and the
 * Jneopallium industrial signal types. No I/O, no shared state — easy
 * to unit-test.
 *
 * <p>Quality propagates per spec §0.5: a {@code DataValue} whose
 * {@link StatusCode} is not {@code good()} produces a signal with
 * {@link Quality#UNCERTAIN} or {@link Quality#BAD} — never {@code GOOD}.
 *
 * <p>Wall-clock timestamps come from OPC UA per §0.6:
 * {@code sourceTime} → {@code serverTime} → local clock.
 */
public final class OpcUaSignalMapper {

    private OpcUaSignalMapper() {}

    public static MeasurementSignal toMeasurement(OpcUaNodeBinding b, DataValue dv) {
        double value = coerceDouble(dv.getValue());
        Quality q = mapQuality(dv.getStatusCode());
        long ts = pickTimestamp(dv);
        return new MeasurementSignal(b.signalTag, value, q, ts);
    }

    public static AlarmSignal toAlarm(OpcUaNodeBinding b, DataValue dv) {
        int code = coerceInt(dv.getValue());
        AlarmPriority pri = mapAlarmPriority(code);
        long ts = pickTimestamp(dv);
        return new AlarmSignal(pri, b.signalTag, "OPCUA-" + code, ts);
    }

    public static DataValue toDataValue(SetpointSignal s) {
        return DataValue.valueOnly(Variant.ofDouble(s.getSetpoint()));
    }

    public static DataValue toDataValue(ActuatorCommandSignal s) {
        return DataValue.valueOnly(Variant.ofDouble(s.getTargetValue()));
    }

    public static DataValue toDataValue(double value) {
        return DataValue.valueOnly(Variant.ofDouble(value));
    }

    static Quality mapQuality(StatusCode sc) {
        if (sc == null) return Quality.UNCERTAIN;
        if (sc.isGood()) return Quality.GOOD;
        if (sc.isBad()) return Quality.BAD;
        return Quality.UNCERTAIN;
    }

    static AlarmPriority mapAlarmPriority(int code) {
        if (code >= 700) return AlarmPriority.URGENT;
        if (code >= 400) return AlarmPriority.HIGH;
        if (code >= 100) return AlarmPriority.LOW;
        return AlarmPriority.JOURNAL;
    }

    static long pickTimestamp(DataValue dv) {
        DateTime src = dv.getSourceTime();
        if (src != null && src.isNotNull()) return src.getJavaTime();
        DateTime srv = dv.getServerTime();
        if (srv != null && srv.isNotNull()) return srv.getJavaTime();
        return System.currentTimeMillis();
    }

    static double coerceDouble(Variant v) {
        if (v == null) return Double.NaN;
        Object o = v.getValue();
        if (o == null) return Double.NaN;
        if (o instanceof Number n) return n.doubleValue();
        if (o instanceof Boolean b) return b ? 1.0 : 0.0;
        throw new IllegalStateException(
                "Cannot coerce OPC UA value to double: " + o.getClass().getName());
    }

    static int coerceInt(Variant v) {
        if (v == null) return 0;
        Object o = v.getValue();
        if (o == null) return 0;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof Boolean b) return b ? 1 : 0;
        throw new IllegalStateException(
                "Cannot coerce OPC UA value to int: " + o.getClass().getName());
    }
}
