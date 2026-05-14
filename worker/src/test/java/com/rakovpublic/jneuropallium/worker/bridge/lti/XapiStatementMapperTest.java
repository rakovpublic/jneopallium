/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.lti;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.AffectObservationSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.EngagementSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.MasteryUpdateSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.tutoring.ResponseSignal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XapiStatementMapperTest {

    private final ObjectMapper json = new ObjectMapper();

    private XapiStatementMapper newMapper(boolean redact) {
        return new XapiStatementMapper(
                new PseudonymService(true, "test-salt"), redact);
    }

    @Test
    void mapsAttemptedToResponseSignal() throws Exception {
        XapiStatementMapper m = newMapper(false);
        JsonNode stmt = json.readTree("""
                {
                  "actor": { "account": { "name": "alice@example.edu" } },
                  "verb":  { "id": "http://adlnet.gov/expapi/verbs/attempted" },
                  "object":{ "id": "item-42", "objectType": "Activity" },
                  "result":{ "success": true, "duration": "PT12.34S",
                             "response": "I think it is X" }
                }
                """);
        ResponseSignal r = m.mapResponse(stmt);
        assertNotNull(r);
        assertEquals("item-42", r.getItemId());
        assertTrue(r.isCorrect());
        assertEquals(12340L, r.getLatencyMs());
        // redactFreeText=false → response payload preserved
        assertEquals("I think it is X", r.getResponsePayload());
        assertFalse("alice@example.edu".equals(r.getName()),
                "raw actor must not appear on emitted signal");
    }

    @Test
    void responseFreeTextIsRedactedByDefault() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode stmt = json.readTree("""
                {
                  "actor": { "mbox": "mailto:alice@example.edu" },
                  "verb":  { "id": "http://adlnet.gov/expapi/verbs/answered" },
                  "object":{ "id": "item-1" },
                  "result":{ "success": false,
                             "response": "secret answer text" }
                }
                """);
        ResponseSignal r = m.mapResponse(stmt);
        assertNotNull(r);
        assertNull(r.getResponsePayload(), "response text must be redacted");
        assertFalse(r.isCorrect());
        assertEquals(1, m.drainRedactionCount());
    }

    @Test
    void masteredVerbBecomesMasteryAtOne() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode stmt = json.readTree("""
                {
                  "actor": { "openid": "https://op.example/u/alice" },
                  "verb":  { "id": "http://adlnet.gov/expapi/verbs/mastered" },
                  "object":{ "id": "competency-7" }
                }
                """);
        MasteryUpdateSignal mu = m.mapMastery(stmt);
        assertNotNull(mu);
        assertEquals("competency-7", mu.getConceptId());
        assertEquals(1.0, mu.getNewMasteryLevel(), 1e-9);
    }

    @Test
    void scaledScoreOverridesVerbDefault() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode stmt = json.readTree("""
                {
                  "actor": { "name": "alice" },
                  "verb":  { "id": "http://adlnet.gov/expapi/verbs/failed" },
                  "object":{ "id": "competency-7" },
                  "result":{ "score": { "scaled": 0.4 } }
                }
                """);
        MasteryUpdateSignal mu = m.mapMastery(stmt);
        assertNotNull(mu);
        assertEquals(0.4, mu.getNewMasteryLevel(), 1e-9);
    }

    @Test
    void interactedBecomesEngagementWithDwell() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode stmt = json.readTree("""
                {
                  "actor": { "name": "alice" },
                  "verb":  { "id": "http://adlnet.gov/expapi/verbs/interacted" },
                  "object":{ "id": "module-1" },
                  "result":{ "duration": "PT5M" }
                }
                """);
        EngagementSignal e = m.mapEngagement(stmt);
        assertNotNull(e);
        // verb prior 0.8 averaged with dwell 0.5 = 0.65
        assertEquals(0.65, e.getAttentionScore(), 1e-6);
    }

    @Test
    void affectExtensionBecomesAffectSignal() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode stmt = json.readTree("""
                {
                  "actor": { "name": "alice" },
                  "verb":  { "id": "http://adlnet.gov/expapi/verbs/experienced" },
                  "object":{ "id": "module-1" },
                  "context": { "extensions": {
                      "https://jneopallium.rakov.org/xapi/extensions/affect": {
                          "valence": 0.3, "arousal": 0.7, "confidence": 0.9
                      }
                  } }
                }
                """);
        AffectObservationSignal a = m.mapAffect(stmt);
        assertNotNull(a);
        assertEquals(0.3, a.getValence(), 1e-9);
        assertEquals(0.7, a.getArousal(), 1e-9);
        assertEquals(0.9, a.getConfidence(), 1e-9);
    }

    @Test
    void statementsHelperWalksBothShapes() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode arr = json.readTree("""
                [
                  { "verb": { "id": "x" } },
                  { "verb": { "id": "y" } }
                ]
                """);
        JsonNode obj = json.readTree("""
                { "statements": [
                    { "verb": { "id": "x" } },
                    { "verb": { "id": "y" } } ] }
                """);
        assertEquals(2, m.statements(arr).size());
        assertEquals(2, m.statements(obj).size());
    }

    @Test
    void unknownVerbReturnsNullForResponseMapper() throws Exception {
        XapiStatementMapper m = newMapper(true);
        JsonNode stmt = json.readTree("""
                { "actor": { "name": "alice" },
                  "verb":  { "id": "http://example.org/verbs/danced" },
                  "object":{ "id": "item-1" } }
                """);
        assertNull(m.mapResponse(stmt),
                "non-response verbs must not be mistakenly mapped to ResponseSignal");
    }
}
