/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import java.util.Arrays;
import java.util.Objects;

/**
 * One raw CAN frame as observed on the bus (13-CANOPEN.md §4). The
 * COB-ID is the 11-bit (or 29-bit extended) identifier; the payload is at
 * most 8 bytes for classic CAN. The bridge derives the CANopen meaning of
 * the frame (PDO / SDO / EMCY / heartbeat / NMT / SYNC) from the COB-ID
 * range, not from the payload.
 *
 * <p>A {@code CanFrame} is what the platform-specific
 * {@link CanopenClientService} emits inbound; the orchestrator decodes it.
 */
public record CanFrame(int cobId, byte[] data, boolean extended, long arrivalTimeMs) {

    public CanFrame {
        Objects.requireNonNull(data, "data");
        if (data.length > 8) {
            throw new IllegalArgumentException(
                    "Classic CAN payload must be <= 8 bytes, got " + data.length);
        }
        data = data.clone();
    }

    public CanFrame(int cobId, byte[] data) {
        this(cobId, data, false, System.currentTimeMillis());
    }

    @Override public byte[] data() { return data.clone(); }

    /** Decoded CANopen function code (top 4 bits of an 11-bit COB-ID). */
    public int functionCode() { return (cobId >>> 7) & 0x0F; }

    /** Decoded source/target node id (low 7 bits of an 11-bit COB-ID). */
    public int nodeId() { return cobId & 0x7F; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("CanFrame{cobId=0x");
        sb.append(Integer.toHexString(cobId)).append(", data=").append(Arrays.toString(data));
        sb.append(", ext=").append(extended).append('}');
        return sb.toString();
    }
}
