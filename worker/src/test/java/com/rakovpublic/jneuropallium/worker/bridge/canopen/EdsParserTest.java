/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link EdsParser} (13-CANOPEN.md §10 S8).
 */
class EdsParserTest {

    /** §10 S8 — a real EDS-style fragment is parsed; OD type info is attached. */
    @Test
    void parsesStatuswordAndPositionActual() throws Exception {
        // Trimmed Maxon EPOS4-style EDS fragment. CiA-301 §7.4.7 data-type IDs:
        // 0x0006 = UINT16, 0x0004 = INT32.
        String eds = """
                [FileInfo]
                FileName=test.eds

                [DeviceInfo]
                VendorName=Test

                [6041]
                ParameterName=Statusword
                DataType=0x0006
                AccessType=ro

                [6064]
                ParameterName=Position actual value
                DataType=0x0004
                AccessType=ro

                [6081]
                ParameterName=Profile velocity
                DataType=0x0007
                AccessType=rw
                DefaultValue=1000

                [Comments]
                Lines=0
                """;
        Map<Integer, ObjectDictionaryEntry> od = EdsParser.parse(eds);

        ObjectDictionaryEntry sw = od.get(0x6041 << 8);
        assertNotNull(sw, "statusword entry must be discovered");
        assertEquals(CanopenBridgeConfig.OdType.UINT16, sw.odType());
        assertEquals(ObjectDictionaryEntry.OdAccess.RO, sw.access());

        ObjectDictionaryEntry pos = od.get(0x6064 << 8);
        assertNotNull(pos);
        assertEquals(CanopenBridgeConfig.OdType.INT32, pos.odType());

        ObjectDictionaryEntry pv = od.get(0x6081 << 8);
        assertNotNull(pv);
        assertEquals(CanopenBridgeConfig.OdType.UINT32, pv.odType());
        assertEquals(ObjectDictionaryEntry.OdAccess.RW, pv.access());
        assertEquals("1000", pv.defaultValue());
    }

    @Test
    void parsesSubObjects() throws Exception {
        String eds = """
                [1018sub1]
                ParameterName=VendorID
                DataType=0x0007
                AccessType=ro
                """;
        Map<Integer, ObjectDictionaryEntry> od = EdsParser.parse(eds);
        ObjectDictionaryEntry e = od.get((0x1018 << 8) | 1);
        assertNotNull(e);
        assertEquals(1, e.subIndex());
        assertEquals(CanopenBridgeConfig.OdType.UINT32, e.odType());
    }

    /** §10 R2 — unknown / vendor data types are dropped (rather than rejecting the whole file). */
    @Test
    void skipsUnknownDataTypes() throws Exception {
        String eds = """
                [2001]
                ParameterName=VendorBlob
                DataType=0x000F
                AccessType=ro
                """;
        Map<Integer, ObjectDictionaryEntry> od = EdsParser.parse(eds);
        assertNull(od.get(0x2001 << 8));
        assertTrue(od.isEmpty());
    }
}
