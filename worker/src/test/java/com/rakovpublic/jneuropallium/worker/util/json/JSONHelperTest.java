package com.rakovpublic.jneuropallium.worker.util.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JSONHelperTest {

    private final JSONHelper helper = new JSONHelper();

    @Test
    void testExtractField_simpleString() {
        String json = "{\"name\":\"alice\",\"age\":\"30\"}";
        assertEquals("alice", helper.extractField(json, "name"));
    }

    @Test
    void testExtractField_numericValueAsString() {
        String json = "{\"count\":\"42\"}";
        assertEquals("42", helper.extractField(json, "count"));
    }

    @Test
    void testExtractField_missingField_throwsException() {
        String json = "{\"name\":\"alice\"}";
        assertThrows(Exception.class, () -> helper.extractField(json, "nonexistent"));
    }

    @Test
    void testExtractField_emptyJson_throwsException() {
        assertThrows(Exception.class, () -> helper.extractField("{}", "field"));
    }

    @Test
    void testExtractField_invalidJson_throwsException() {
        assertThrows(Exception.class, () -> helper.extractField("not-json", "field"));
    }
}
