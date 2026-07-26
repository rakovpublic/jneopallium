/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;

/**
 * {@link LslBridgeConfigLoader} unit tests (05-LSL.md §6, §10 R3).
 */
class LslBridgeConfigLoaderTest {

    @Test
    void loadsMinimalConfig() throws Exception {
        String yaml = """
                discovery:
                  resolveTimeoutMs: 2000
                  expectedStreams:
                    - OpenViBE-EEG-256Hz
                reads:
                  - bindingId: EEG-MAIN
                    streamName: OpenViBE-EEG-256Hz
                    streamType: EEG
                    channels: [Cz, Fz, Pz]
                    chunkLengthSamples: 64
                    targetSignal: LFP
                    signalTagPrefix: BCI.EEG
                writes:
                  - bindingId: STIM-ADVISORY
                    outletName: Jneopallium-Stim-Advisory
                    type: MARKERS
                    nominalSrate: 0
                    signalTag: BCI.STIM.ADVISORY
                    stimulationGated: true
                audit:
                  localAuditFile: /tmp/lsl-audit.jsonl
                perTagSafetyMode:
                  STIM-ADVISORY: ADVISORY
                """;
        LslBridgeConfig cfg = LslBridgeConfigLoader.load(yaml);
        assertEquals(1, cfg.reads().size());
        assertEquals(1, cfg.writes().size());
        assertEquals("EEG-MAIN", cfg.reads().get(0).bindingId());
        assertEquals(LslBridgeConfig.ReadSignalKind.LFP, cfg.reads().get(0).targetSignal());
        assertEquals(LslBridgeConfig.OutletKind.MARKERS, cfg.writes().get(0).type());
        assertTrue(cfg.writes().get(0).stimulationGated());
        assertEquals(BridgeSafetyMode.ADVISORY, cfg.perTagSafetyMode().get("STIM-ADVISORY"));
    }

    /** Unknown YAML keys are rejected at load (00-FRAMEWORK §3). */
    @Test
    void unknownKeyIsRejected() {
        String yaml = """
                discovery:
                  resolveTimeoutMs: 2000
                audit:
                  localAuditFile: /tmp/x.jsonl
                bogusKey: 42
                """;
        assertThrows(UnrecognizedPropertyException.class, () -> LslBridgeConfigLoader.load(yaml));
    }

    /**
     * 05-LSL.md §6 — {@code AUTONOMOUS} is structurally rejected for any
     * LSL write binding (the bridge ceiling is ADVISORY).
     */
    @Test
    void autonomousModeRejectedOnWriteBinding() {
        String yaml = """
                discovery:
                  resolveTimeoutMs: 1000
                writes:
                  - bindingId: STIM-ADVISORY
                    outletName: Jneopallium-Stim-Advisory
                    type: MARKERS
                    nominalSrate: 0
                    signalTag: BCI.STIM.ADVISORY
                    stimulationGated: true
                audit:
                  localAuditFile: /tmp/lsl-audit.jsonl
                perTagSafetyMode:
                  STIM-ADVISORY: AUTONOMOUS
                """;
        Throwable t = assertThrows(Throwable.class, () -> LslBridgeConfigLoader.load(yaml));
        // The IllegalArgumentException raised by the record constructor is
        // wrapped by Jackson; verify the message is preserved.
        assertNotNull(t);
        assertTrue(t.getMessage().contains("AUTONOMOUS")
                || (t.getCause() != null && t.getCause().getMessage().contains("AUTONOMOUS")));
    }

    /** Duplicate bindingIds are rejected. */
    @Test
    void duplicateBindingIdsRejected() {
        String yaml = """
                discovery:
                  resolveTimeoutMs: 1000
                reads:
                  - bindingId: A
                    streamName: S1
                    streamType: EEG
                    targetSignal: LFP
                  - bindingId: A
                    streamName: S2
                    streamType: EEG
                    targetSignal: LFP
                audit:
                  localAuditFile: /tmp/x.jsonl
                """;
        Throwable t = assertThrows(Throwable.class, () -> LslBridgeConfigLoader.load(yaml));
        assertTrue(t.getMessage().contains("duplicate")
                || (t.getCause() != null && t.getCause().getMessage().contains("duplicate")));
    }
}
