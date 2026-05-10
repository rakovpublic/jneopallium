/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import java.util.Objects;

/**
 * One CANopen Object Dictionary entry parsed from an EDS / DCF
 * (13-CANOPEN.md §1, §6, §10 S8). The (index, subIndex) pair is the
 * key the bridge uses to look up the type and access flags when decoding
 * a PDO byte payload.
 *
 * <p>The entry is intentionally minimal: only the fields the decoder /
 * encoder need ({@link OdAccess}, {@link CanopenBridgeConfig.OdType},
 * default value, parameter name) are kept. Unknown / vendor-specific keys
 * are dropped by {@link EdsParser} with a warning.
 */
public record ObjectDictionaryEntry(
        int index,
        int subIndex,
        String parameterName,
        CanopenBridgeConfig.OdType odType,
        OdAccess access,
        String defaultValue
) {
    public ObjectDictionaryEntry {
        Objects.requireNonNull(odType, "odType");
        if (access == null) access = OdAccess.RW;
    }

    /** CiA-301 §7.4.5 access flags. */
    public enum OdAccess { RO, WO, RW, RWR, RWW, CONST }
}
