/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CanopenBridgeConfigLoader} (13-CANOPEN.md §6, §10 S11).
 */
class CanopenBridgeConfigLoaderTest {

    @Test
    void loadsMinimalConfig() throws Exception {
        String yaml = """
                canBus:
                  type: SOCKETCAN
                  device: vcan0
                  bitrate: 500000
                nodes:
                  - id: 1
                    type: CiA-402
                writeIndexAllowList:
                  1:
                    - 24705   # 0x6081
                writes:
                  - bindingId: DRIVE-1-PROFILE-VEL
                    nodeId: 1
                    odIndex: 24705
                    subIndex: 0
                    odType: UINT32
                    via: SDO
                    signalTag: DRIVE.1.PROFILE_VEL
                    minClampValue: 0
                    maxClampValue: 5000
                audit:
                  localAuditFile: /tmp/canopen-audit.jsonl
                """;
        CanopenBridgeConfig cfg = CanopenBridgeConfigLoader.load(yaml);
        assertEquals(CanopenBridgeConfig.BusType.SOCKETCAN, cfg.canBus().type());
        assertEquals(1, cfg.writes().size());
        assertEquals("DRIVE-1-PROFILE-VEL", cfg.writes().get(0).bindingId());
    }

    /** §10 S11 — controlword (0x6040) is on the FORBIDDEN list and rejected at load. */
    @Test
    void rejectsControlwordWriteBinding() {
        String yaml = """
                canBus:
                  type: SOCKETCAN
                  device: vcan0
                  bitrate: 500000
                nodes:
                  - id: 1
                writeIndexAllowList:
                  1:
                    - 24640   # 0x6040 — controlword, intentionally allow-listed to prove the FORBIDDEN backstop
                writes:
                  - bindingId: BAD
                    nodeId: 1
                    odIndex: 24640
                    odType: UINT16
                    via: SDO
                    signalTag: BAD.CTRLWORD
                audit:
                  localAuditFile: /tmp/x
                """;
        Exception ex = assertThrows(Exception.class, () -> CanopenBridgeConfigLoader.load(yaml));
        assertTrue(ex.getMessage().contains("forbidden") || rootMessage(ex).contains("forbidden"),
                "expected forbidden-OD-index message, got: " + rootMessage(ex));
    }

    /** §10 S11 — write bindings whose (nodeId, odIndex) is not on writeIndexAllowList are rejected. */
    @Test
    void rejectsWriteBindingNotInAllowList() {
        String yaml = """
                canBus:
                  type: SOCKETCAN
                  device: vcan0
                  bitrate: 500000
                nodes:
                  - id: 1
                writeIndexAllowList:
                  1:
                    - 24705
                writes:
                  - bindingId: NOT-LISTED
                    nodeId: 1
                    odIndex: 24706
                    odType: UINT32
                    via: SDO
                    signalTag: NOT.LISTED
                audit:
                  localAuditFile: /tmp/x
                """;
        Exception ex = assertThrows(Exception.class, () -> CanopenBridgeConfigLoader.load(yaml));
        assertTrue(rootMessage(ex).contains("writeIndexAllowList"),
                "expected allow-list message, got: " + rootMessage(ex));
    }

    /** 00-FRAMEWORK §3 — unknown YAML keys must be rejected at load. */
    @Test
    void rejectsUnknownYamlKey() {
        String yaml = """
                canBus:
                  type: SOCKETCAN
                  device: vcan0
                  bitrate: 500000
                gravityWell: 9.8
                audit:
                  localAuditFile: /tmp/x
                """;
        assertThrows(Exception.class, () -> CanopenBridgeConfigLoader.load(yaml));
    }

    private static String rootMessage(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) cur = cur.getCause();
        return cur.getMessage() == null ? t.toString() : cur.getMessage();
    }
}
