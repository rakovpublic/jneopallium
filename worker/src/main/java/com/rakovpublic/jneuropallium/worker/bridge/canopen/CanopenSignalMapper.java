/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.BatchPhase;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.embodiment.ProprioceptiveSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.BatchStateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Pure functions: CANopen PDO / EMCY / heartbeat ↔ Jneopallium typed
 * signal (13-CANOPEN.md §5).
 *
 * <p>The mapper does not own state — it takes a binding plus already-decoded
 * frame fields and returns a typed signal. Routing (COB-ID → binding) and
 * the per-node liveness tracker live in {@link CanopenClientService}.
 */
public final class CanopenSignalMapper {

    /** CiA-301 §7.2.1 EMCY error-code → severity. The full CANopen table is huge;
     *  we collapse it onto five buckets matching the 13-CANOPEN.md §10 S10 brief. */
    public static AlarmPriority emcySeverity(int errorCode) {
        // 0x0000 — no error / reset notification.
        if (errorCode == 0x0000) return AlarmPriority.JOURNAL;
        // CANopen groups error codes by the upper nibble of the upper byte
        // (CiA-301 §7.2.1.2 EMCY, table 17). 0x2xxx = current (overcurrent /
        // short circuit) — kinetic-energy hazard, escalate to URGENT.
        return switch ((errorCode >>> 12) & 0xf) {
            case 0x1 -> AlarmPriority.LOW;       // generic
            case 0x2 -> AlarmPriority.URGENT;    // current
            case 0x3, 0x4, 0x5, 0x6, 0x8 -> AlarmPriority.HIGH; // voltage/temp/hw/sw/comm
            case 0xf -> AlarmPriority.HIGH;      // vendor-specific — don't silently swallow
            default -> AlarmPriority.LOW;
        };
    }

    /** Standard CANopen EMCY error-code → human label (subset; §10 S10). */
    public static String emcyLabel(int errorCode) {
        return switch (errorCode) {
            case 0x0000 -> "ERROR_RESET_OR_NO_ERROR";
            case 0x1000 -> "GENERIC_ERROR";
            case 0x2310 -> "CONTINUOUS_OVER_CURRENT";
            case 0x2320 -> "SHORT_CIRCUIT_INTERNAL";
            case 0x3110 -> "MAINS_OVER_VOLTAGE";
            case 0x3120 -> "MAINS_UNDER_VOLTAGE";
            case 0x3210 -> "DC_LINK_OVER_VOLTAGE";
            case 0x3220 -> "DC_LINK_UNDER_VOLTAGE";
            case 0x4210 -> "EXCESS_TEMPERATURE_DEVICE";
            case 0x5530 -> "EEPROM_FAULT";
            case 0x6010 -> "SOFTWARE_RESET_WATCHDOG";
            case 0x8110 -> "CAN_OVERRUN";
            case 0x8130 -> "LIFEGUARD_HEARTBEAT_ERROR";
            case 0x8140 -> "RECOVERED_FROM_BUS_OFF";
            case 0x8210 -> "PDO_NOT_PROCESSED";
            default -> "EMCY_0x" + Integer.toHexString(errorCode).toUpperCase();
        };
    }

    /** Decode a scalar from a PDO payload according to the binding's OD type, scale, offset. */
    public double decodeScalar(CanopenNodeBinding b, byte[] payload) {
        long raw = readRaw(b.odType(), payload, 0);
        double v = b.odType() == CanopenBridgeConfig.OdType.REAL32
                ? Float.intBitsToFloat((int) raw)
                : (double) raw;
        return v * b.scale() + b.offset();
    }

    /** Encode a scalar back to bytes for an SDO/RPDO write. */
    public byte[] encodeScalar(CanopenBridgeConfig.OdType type, double value) {
        return switch (type) {
            case UINT8 -> new byte[]{(byte) ((int) value & 0xff)};
            case INT8 -> new byte[]{(byte) ((int) value & 0xff)};
            case UINT16, INT16 -> {
                ByteBuffer bb = ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN);
                bb.putShort((short) ((int) value & 0xffff));
                yield bb.array();
            }
            case UINT32, INT32 -> {
                ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                bb.putInt((int) value);
                yield bb.array();
            }
            case REAL32 -> {
                ByteBuffer bb = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
                bb.putFloat((float) value);
                yield bb.array();
            }
        };
    }

    /** Read the raw integer (or REAL32 bit pattern) from a payload at offset. */
    public static long readRaw(CanopenBridgeConfig.OdType type, byte[] payload, int offset) {
        if (payload == null || offset >= payload.length) return 0L;
        ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        bb.position(offset);
        return switch (type) {
            case UINT8 -> bb.get() & 0xffL;
            case INT8 -> (long) bb.get();
            case UINT16 -> bb.getShort() & 0xffffL;
            case INT16 -> (long) bb.getShort();
            case UINT32 -> bb.getInt() & 0xffffffffL;
            case INT32 -> (long) bb.getInt();
            case REAL32 -> ((long) bb.getInt()) & 0xffffffffL;
        };
    }

    /** PDO scalar → ProprioceptiveSignal (own joint state). */
    public IInputSignal toProprioceptive(CanopenNodeBinding b, double value, long ts) {
        ProprioceptiveSignal s = new ProprioceptiveSignal(
                b.nodeId(), new double[]{value}, ts);
        s.setName(b.signalTag() == null ? b.bindingId() : b.signalTag());
        return s;
    }

    /** PDO scalar → MeasurementSignal (CiA-401 I/O, sensor PDO). */
    public IInputSignal toMeasurement(CanopenNodeBinding b, double value, Quality q, long ts) {
        MeasurementSignal s = new MeasurementSignal(
                b.signalTag() == null ? b.bindingId() : b.signalTag(),
                value, q, ts);
        return s;
    }

    /** PDO scalar → EfficiencySignal (BMS). The mapper assumes 0..100 % SoC inputs. */
    public IInputSignal toEfficiency(CanopenNodeBinding b, double value) {
        String unit = b.signalTag() == null ? b.bindingId() : b.signalTag();
        double pct = value > 1.0 ? value / 100.0 : value;
        return new EfficiencySignal(unit, pct, 1.0);
    }

    /** Statusword (CiA-402 0x6041) → BatchStateSignal + optional fault AlarmSignal. */
    public BatchStateSignal toDriveState(CanopenNodeBinding b, int statusword, long ts) {
        BatchPhase phase = decodeStatuswordPhase(statusword);
        Map<String, Double> metrics = new LinkedHashMap<>();
        metrics.put("statusword", (double) (statusword & 0xffff));
        metrics.put("ts", (double) ts);
        BatchStateSignal s = new BatchStateSignal(
                "DRIVE." + b.nodeId(), phase, metrics);
        s.setName(b.signalTag() == null ? b.bindingId() : b.signalTag());
        return s;
    }

    /** Bit 3 of statusword is FAULT — flip it into an AlarmSignal so the safety chain sees it. */
    public AlarmSignal statuswordFault(CanopenNodeBinding b, int statusword, long ts) {
        if ((statusword & 0x0008) == 0) return null;
        String tag = (b.signalTag() == null ? b.bindingId() : b.signalTag()) + ".FAULT";
        return new AlarmSignal(AlarmPriority.HIGH, tag,
                "DRIVE_FAULT:0x" + Integer.toHexString(statusword & 0xffff), ts);
    }

    /** EMCY frame (8 bytes) → AlarmSignal. */
    public AlarmSignal toEmcyAlarm(CanopenNodeBinding b, byte[] payload, long ts) {
        if (payload == null || payload.length < 3) return null;
        ByteBuffer bb = ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN);
        int errorCode = bb.getShort() & 0xffff;
        int errorReg = bb.get() & 0xff;
        AlarmPriority pri = emcySeverity(errorCode);
        String label = emcyLabel(errorCode);
        String prefix = b.signalTagPrefix() != null ? b.signalTagPrefix() : "DRIVE.FAULT";
        String tag = prefix + "." + b.nodeId();
        return new AlarmSignal(pri, tag,
                label + ":errorReg=0x" + Integer.toHexString(errorReg), ts);
    }

    /** Heartbeat-loss (synthesised by the bridge watchdog, §5, §10 S9). */
    public AlarmSignal nodeOffline(int nodeId, long ts) {
        return new AlarmSignal(AlarmPriority.HIGH,
                "NODE." + nodeId, "NODE_OFFLINE", ts);
    }

    /**
     * Heartbeat-loss measurement quality flag (§5: "subsequent reads marked
     * Quality.UNCERTAIN" — propagated through the latest-value cache).
     */
    public static Quality offlineQuality() { return Quality.UNCERTAIN; }

    /** CiA-402 statusword → CiA-88 batch phase (compact mapping for the bridge audit). */
    static BatchPhase decodeStatuswordPhase(int sw) {
        // Bits 0..6 carry the state-machine masked subset. CiA-402 §6.3.
        int masked = sw & 0x6f;
        // Fault.
        if ((sw & 0x0008) != 0) return BatchPhase.ABORTED;
        // Operation enabled = 0x27.
        if ((masked & 0x27) == 0x27) return BatchPhase.RUNNING;
        // Switched on = 0x23.
        if ((masked & 0x23) == 0x23) return BatchPhase.HELD;
        // Ready to switch on = 0x21.
        if ((masked & 0x21) == 0x21) return BatchPhase.IDLE;
        // Quick stop active = 0x07.
        if ((masked & 0x07) == 0x07) return BatchPhase.STOPPED;
        return BatchPhase.IDLE;
    }
}
