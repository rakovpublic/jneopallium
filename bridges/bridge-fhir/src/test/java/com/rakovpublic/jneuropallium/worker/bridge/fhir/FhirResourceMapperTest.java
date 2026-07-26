/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fhir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.AlertSeverity;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.VitalType;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.AdverseEventAlertSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DemographicSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.DiagnosisHypothesisSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.LabResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.MedicationAdminSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.VitalSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FhirResourceMapper} tests (06-FHIR.md §5 mapping table,
 * §9 S9 / S11).
 */
class FhirResourceMapperTest {

    private final ObjectMapper json = new ObjectMapper();
    private final PseudonymService pseudo = new PseudonymService(true, "test-salt");

    @Test
    void mapsHeartRateVital() throws Exception {
        String obs = """
                {
                  "resourceType": "Observation",
                  "subject": { "reference": "Patient/abc" },
                  "code": { "coding": [{ "system": "http://loinc.org", "code": "8867-4" }] },
                  "effectiveDateTime": "2026-01-01T12:00:00Z",
                  "valueQuantity": { "value": 78, "unit": "/min" }
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        VitalSignal v = m.mapVital(json.readTree(obs));
        assertNotNull(v);
        assertEquals(VitalType.HR, v.getType());
        assertEquals(78.0, v.getMeasurement(), 1e-9);
        // §3 rule 3 — raw pid never reaches the signal.
        assertFalse("abc".equals(v.getPatientId()),
                "raw patient id 'abc' must not appear on the signal");
        assertNotNull(v.getPatientId());
    }

    @Test
    void mapsGlucoseLabResultWithReferenceRange() throws Exception {
        String obs = """
                {
                  "resourceType": "Observation",
                  "subject": { "reference": "Patient/xyz" },
                  "code": { "coding": [{ "system": "http://loinc.org", "code": "2339-0" }] },
                  "effectiveDateTime": "2026-01-01T12:00:00Z",
                  "valueQuantity": { "value": 142.0, "unit": "mg/dL" },
                  "referenceRange": [ { "low": { "value": 70 }, "high": { "value": 110 } } ]
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        LabResultSignal lab = m.mapLab(json.readTree(obs));
        assertNotNull(lab);
        assertEquals("2339-0", lab.getAnalyteCode());
        assertEquals(142.0, lab.getMeasurement(), 1e-9);
        assertEquals("mg/dL", lab.getUnits());
        assertNotNull(lab.getReferenceRange());
        assertEquals(70.0, lab.getReferenceRange()[0]);
        assertEquals(110.0, lab.getReferenceRange()[1]);
        assertTrue(lab.isAbnormal(), "142 > 110 → abnormal");
    }

    @Test
    void mapsPatientToDemographic() throws Exception {
        String pat = """
                {
                  "resourceType": "Patient",
                  "id": "example-pid-1",
                  "gender": "female",
                  "birthDate": "1985-04-12"
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        DemographicSignal d = m.mapPatient(json.readTree(pat));
        assertNotNull(d);
        assertEquals(com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.Sex.FEMALE, d.getSex());
        assertTrue(d.getAgeYears() >= 30);
        assertNotNull(d.getPatientId());
        assertFalse("example-pid-1".equals(d.getPatientId()),
                "raw patient id must not leak into the demographic signal");
    }

    @Test
    void mapsConditionToDiagnosis() throws Exception {
        String cond = """
                {
                  "resourceType": "Condition",
                  "subject": { "reference": "Patient/p1" },
                  "code": { "coding": [{ "system": "http://hl7.org/fhir/sid/icd-10", "code": "E11.9" }] },
                  "clinicalStatus": { "coding": [{ "code": "active" }] }
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        DiagnosisHypothesisSignal d = m.mapCondition(json.readTree(cond));
        assertNotNull(d);
        assertEquals("E11.9", d.getIcd10());
        assertTrue(d.getPosteriorProbability() >= 0.9, "active → strong prior");
    }

    @Test
    void mapsMedicationAdministration() throws Exception {
        String med = """
                {
                  "resourceType": "MedicationAdministration",
                  "subject": { "reference": "Patient/p1" },
                  "medicationCodeableConcept": {
                    "coding": [{
                      "system": "http://www.nlm.nih.gov/research/umls/rxnorm",
                      "code": "5856"
                    }]
                  },
                  "effectiveDateTime": "2026-01-01T12:00:00Z",
                  "dosage": {
                    "dose": { "value": 10, "unit": "IU" },
                    "route": { "coding": [{ "code": "SC" }] }
                  }
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        MedicationAdminSignal ma = m.mapMedicationAdministration(json.readTree(med));
        assertNotNull(ma);
        assertEquals("5856", ma.getRxNormCode());
        assertEquals(10.0, ma.getDose(), 1e-9);
        assertEquals("IU", ma.getUnits());
        assertEquals("SC", ma.getRoute());
    }

    @Test
    void mapsAllergyIntolerance() throws Exception {
        String allergy = """
                {
                  "resourceType": "AllergyIntolerance",
                  "patient": { "reference": "Patient/p1" },
                  "code": { "coding": [{ "code": "penicillin" }] },
                  "criticality": "high"
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        AdverseEventAlertSignal a = m.mapAllergyIntolerance(json.readTree(allergy));
        assertNotNull(a);
        assertEquals(AlertSeverity.CRITICAL, a.getSeverity());
        assertTrue(a.getEventCode().startsWith("ALLERGY"));
    }

    /** §11 S11 — note redaction counter increments without exposing the note content. */
    @Test
    void freeTextNoteIncrementsRedactionCounter() throws Exception {
        String obs = """
                {
                  "resourceType": "Observation",
                  "subject": { "reference": "Patient/p1" },
                  "code": { "coding": [{ "system": "http://loinc.org", "code": "8867-4" }] },
                  "effectiveDateTime": "2026-01-01T12:00:00Z",
                  "valueQuantity": { "value": 80 },
                  "note": [{ "text": "patient mentioned something sensitive" }]
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        assertEquals(0, m.drainRedactionCount());
        m.mapVital(json.readTree(obs));
        assertEquals(1, m.drainRedactionCount());
        assertEquals(0, m.drainRedactionCount(), "drain resets to zero");
    }

    @Test
    void redactionDisabledWhenConfigured() throws Exception {
        String obs = """
                {
                  "resourceType": "Observation",
                  "subject": { "reference": "Patient/p1" },
                  "code": { "coding": [{ "system": "http://loinc.org", "code": "8867-4" }] },
                  "effectiveDateTime": "2026-01-01T12:00:00Z",
                  "valueQuantity": { "value": 80 },
                  "note": [{ "text": "irrelevant" }]
                }
                """;
        FhirResourceMapper m = new FhirResourceMapper(pseudo, false);
        m.mapVital(json.readTree(obs));
        assertEquals(0, m.drainRedactionCount(),
                "when redactFreeText=false, counter must not increment");
    }

    @Test
    void unknownResourceReturnsNull() throws Exception {
        FhirResourceMapper m = new FhirResourceMapper(pseudo, true);
        assertNull(m.mapPatient(json.readTree("{\"resourceType\":\"Observation\"}")));
        assertNull(m.mapCondition(json.readTree("{\"resourceType\":\"Other\"}")));
    }
}
