/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parser-level tests for 07-DICOM.md §5 (SR mapping) and §11 S11 (deeply
 * nested findings).
 */
class DicomSrParserTest {

    private final ObjectMapper json = new ObjectMapper();
    private final DicomSrParser parser = new DicomSrParser();

    @Test
    void parsesFlatFindingsAndExtractsPatientId() throws Exception {
        String srJson = """
                {
                  "00080060": { "vr": "CS", "Value": ["SR"] },
                  "00100020": { "vr": "LO", "Value": ["PT-001"] },
                  "00100010": { "vr": "PN", "Value": [{"Alphabetic": "DOE^JOHN"}] },
                  "00180015": { "vr": "CS", "Value": ["CHEST"] },
                  "0040A730": { "vr": "SQ", "Value": [
                    {
                      "0040A040": { "vr": "CS", "Value": ["CODE"] },
                      "0040A043": { "vr": "SQ", "Value": [{
                        "00080100": { "vr": "SH", "Value": ["F-01"] },
                        "00080102": { "vr": "SH", "Value": ["DCM"] },
                        "00080104": { "vr": "LO", "Value": ["Finding"] }
                      }]},
                      "0040A168": { "vr": "SQ", "Value": [{
                        "00080100": { "vr": "SH", "Value": ["17621005"] },
                        "00080102": { "vr": "SH", "Value": ["SCT"] },
                        "00080104": { "vr": "LO", "Value": ["Normal"] }
                      }]}
                    }
                  ]}
                }
                """;
        DicomSrParser.SrDocument doc = parser.parse(json.readTree(srJson));
        assertEquals("SR", doc.modality());
        assertEquals("PT-001", doc.rawPatientId());
        assertEquals("DOE^JOHN", doc.rawPatientName());
        assertEquals("CHEST", doc.bodyPart());
        assertEquals(1, doc.findings().size());
        DicomSrParser.Finding f = doc.findings().get(0);
        assertEquals("CODE", f.valueType());
        assertEquals("SCT:17621005", f.valueCode());
    }

    /** §11 S11 — 3-level nested SR; all leaves emitted, intermediate nodes not duplicated. */
    @Test
    void deeplyNestedSrEmitsOnlyLeaves() throws Exception {
        String srJson = """
                {
                  "00080060": { "vr": "CS", "Value": ["SR"] },
                  "00100020": { "vr": "LO", "Value": ["PT-002"] },
                  "0040A730": { "vr": "SQ", "Value": [
                    {
                      "0040A040": { "vr": "CS", "Value": ["CONTAINER"] },
                      "0040A730": { "vr": "SQ", "Value": [
                        {
                          "0040A040": { "vr": "CS", "Value": ["CONTAINER"] },
                          "0040A730": { "vr": "SQ", "Value": [
                            {
                              "0040A040": { "vr": "CS", "Value": ["CODE"] },
                              "0040A168": { "vr": "SQ", "Value": [{
                                "00080100": { "vr": "SH", "Value": ["MASS"] },
                                "00080102": { "vr": "SH", "Value": ["SCT"] },
                                "00080104": { "vr": "LO", "Value": ["Mass"] }
                              }]}
                            },
                            {
                              "0040A040": { "vr": "CS", "Value": ["TEXT"] },
                              "0040A160": { "vr": "UT", "Value": ["spiculated"] }
                            }
                          ]}
                        },
                        {
                          "0040A040": { "vr": "CS", "Value": ["NUM"] },
                          "0040A30A": { "vr": "SQ", "Value": [{
                            "0040A30B": { "vr": "DS", "Value": [12.5] }
                          }]}
                        }
                      ]}
                    }
                  ]}
                }
                """;
        DicomSrParser.SrDocument doc = parser.parse(json.readTree(srJson));
        List<DicomSrParser.Finding> leaves = doc.findings();
        assertEquals(3, leaves.size(),
                "S11 — three leaf observations expected, intermediate CONTAINERs should not duplicate");
        assertTrue(leaves.stream().anyMatch(f -> "SCT:MASS".equals(f.valueCode())));
        assertTrue(leaves.stream().anyMatch(f -> "spiculated".equals(f.text())));
        assertTrue(leaves.stream().anyMatch(
                f -> f.numericValue() != null && Math.abs(f.numericValue() - 12.5) < 1e-9));
    }

    /** §10 R3 — unsupported scheme is preserved as opaque code; finding still emits. */
    @Test
    void unknownSchemePassesThroughAsOpaqueCode() throws Exception {
        String srJson = """
                {
                  "00080060": { "vr": "CS", "Value": ["SR"] },
                  "0040A730": { "vr": "SQ", "Value": [
                    {
                      "0040A040": { "vr": "CS", "Value": ["CODE"] },
                      "0040A168": { "vr": "SQ", "Value": [{
                        "00080100": { "vr": "SH", "Value": ["XYZ"] },
                        "00080102": { "vr": "SH", "Value": ["UNKNOWN"] },
                        "00080104": { "vr": "LO", "Value": ["Unknown"] }
                      }]}
                    }
                  ]}
                }
                """;
        DicomSrParser.SrDocument doc = parser.parse(json.readTree(srJson));
        assertEquals(1, doc.findings().size());
        assertEquals("UNKNOWN:XYZ", doc.findings().get(0).valueCode());
        assertFalse(parser.schemeSupported("UNKNOWN"));
    }

    @Test
    void parserHandlesEmptyInputGracefully() {
        DicomSrParser.SrDocument doc = parser.parse(null);
        assertNotNull(doc);
        assertTrue(doc.findings().isEmpty());
    }
}
