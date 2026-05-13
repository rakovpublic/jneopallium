/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 11-IEC61850.md §6 — SCL parser is the ground-truth resolver. Bridge
 * fails fast when the SCL is missing or malformed.
 */
class SclParserTest {

    @Test
    void parsesMinimalSclIntoIeds(@TempDir Path tmp) throws IOException {
        Path scl = tmp.resolve("SUB1.icd");
        Files.writeString(scl, """
                <?xml version="1.0" encoding="UTF-8"?>
                <SCL xmlns="http://www.iec.ch/61850/2003/SCL">
                  <IED name="RELAY-A1">
                    <AccessPoint name="S1">
                      <Server>
                        <LDevice inst="LD0">
                          <LN0 lnClass="LLN0" inst=""/>
                          <LN prefix="" lnClass="MMXU" inst="1">
                            <DOI name="PhV"/>
                          </LN>
                          <LN prefix="" lnClass="XCBR" inst="1">
                            <DOI name="Pos">
                              <DAI name="stVal"/>
                            </DOI>
                          </LN>
                          <LN prefix="" lnClass="PIOC" inst="1"/>
                        </LDevice>
                      </Server>
                    </AccessPoint>
                  </IED>
                </SCL>
                """, StandardCharsets.UTF_8);
        SclParser.SclModel m = SclParser.parseFile(scl);
        assertEquals(1, m.ieds().size());
        assertNotNull(m.iedByName("RELAY-A1"));
        assertEquals(1, m.iedByName("RELAY-A1").devices().size());
        assertTrue(m.logicalNodeClasses().contains("MMXU"));
        assertTrue(m.logicalNodeClasses().contains("XCBR"));
        assertTrue(m.logicalNodeClasses().contains("PIOC"),
                "PIOC class must be visible so the mapper can look up severity");
    }

    @Test
    void resolvesLogicalNodeClassFromDaPath(@TempDir Path tmp) throws IOException {
        Path scl = tmp.resolve("SUB1.icd");
        Files.writeString(scl, """
                <?xml version="1.0"?>
                <SCL>
                  <IED name="R">
                    <AccessPoint name="S1"><Server>
                      <LDevice inst="LD0">
                        <LN0 lnClass="LLN0" inst=""/>
                        <LN lnClass="MMXU" inst="1"/>
                        <LN lnClass="XCBR" inst="1"/>
                      </LDevice>
                    </Server></AccessPoint>
                  </IED>
                </SCL>
                """, StandardCharsets.UTF_8);
        SclParser.SclModel m = SclParser.parseFile(scl);
        assertEquals("MMXU", m.resolveLogicalNodeClass("LD0/MMXU1.PhV.phsA.cVal.mag.f"));
        assertEquals("XCBR", m.resolveLogicalNodeClass("LD0/XCBR1.Pos.stVal"));
    }

    /** §9 S9 — bridge fails fast when SCL is missing. */
    @Test
    void missingSclFileThrowsIoException(@TempDir Path tmp) {
        Path scl = tmp.resolve("nope.icd");
        IOException ex = assertThrows(IOException.class, () -> SclParser.parseFile(scl));
        assertTrue(ex.getMessage().contains("SCL file not found"),
                "expected fail-fast on missing SCL, was: " + ex.getMessage());
    }

    @Test
    void malformedSclFailsFast(@TempDir Path tmp) throws IOException {
        Path scl = tmp.resolve("bad.icd");
        Files.writeString(scl, "<SCL><IED name=\"R\"></SCL>");
        IOException ex = assertThrows(IOException.class, () -> SclParser.parseFile(scl));
        assertTrue(ex.getMessage().contains("malformed SCL"),
                "expected fail-fast on malformed SCL, was: " + ex.getMessage());
    }
}
