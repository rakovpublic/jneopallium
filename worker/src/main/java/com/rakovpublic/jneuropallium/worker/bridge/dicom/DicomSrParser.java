/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.FindingCategory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

/**
 * Walks a DICOM Structured Report (SR) content tree and yields the leaf
 * observations as {@link Finding} records (07-DICOM.md §5, §9 S11).
 *
 * <p>The parser consumes the DICOM+JSON representation returned by
 * WADO-RS metadata: each attribute is keyed by its 8-digit tag and carries
 * a {@code vr} / {@code Value} pair. Container items recurse via the
 * Content Sequence (tag {@code 0040A730}); only non-container leaves emit
 * a {@link Finding}. Intermediate {@code CONTAINER} nodes are <b>not</b>
 * duplicated as findings — S11 requires "all leaves are emitted;
 * intermediate nodes are not duplicated".
 *
 * <p>The parser supports three SR coding schemes by default: SNOMED CT,
 * LOINC and RadLex (§10 R3). Unknown coding schemes are passed through as
 * opaque {@code "SCHEME:code"} strings and the resulting finding carries
 * {@link FindingCategory#INDETERMINATE}.
 */
public final class DicomSrParser {

    /** SR attribute tags (DICOM PS3.6). */
    private static final String TAG_VALUE_TYPE          = "0040A040";
    private static final String TAG_CONCEPT_NAME        = "0040A043";
    private static final String TAG_CONCEPT_CODE        = "0040A168";
    private static final String TAG_TEXT_VALUE          = "0040A160";
    private static final String TAG_MEASURED_VALUE_SEQ  = "0040A30A";
    private static final String TAG_NUMERIC_VALUE       = "0040A30B";
    private static final String TAG_MEASUREMENT_UNITS   = "004008EA";
    private static final String TAG_CONTENT_SEQUENCE    = "0040A730";
    private static final String TAG_REL_TYPE            = "0040A010";

    /** Document-level tags. */
    private static final String TAG_MODALITY            = "00080060";
    private static final String TAG_PATIENT_ID          = "00100020";
    private static final String TAG_PATIENT_NAME        = "00100010";
    private static final String TAG_BODY_PART_EXAMINED  = "00180015";
    private static final String TAG_INSTITUTION_NAME    = "00080080";

    /** Code system OIDs / designators recognised by the parser. */
    private static final String SCHEME_SNOMED = "SCT";
    private static final String SCHEME_SNOMED_OID = "2.16.840.1.113883.6.96";
    private static final String SCHEME_LOINC  = "LN";
    private static final String SCHEME_LOINC_OID = "2.16.840.1.113883.6.1";
    private static final String SCHEME_RADLEX = "RADLEX";
    private static final String SCHEME_DCM    = "DCM";

    /**
     * A leaf observation extracted from an SR content tree.
     *
     * @param conceptName  the SR {@code ConceptNameCodeSequence} (what was
     *                     observed, e.g. "Finding")
     * @param valueCode    the coded value if the leaf is a CODE item, or
     *                     {@code null} for text/numeric leaves
     * @param text         text value if the leaf is a TEXT item
     * @param numericValue numeric value if the leaf is a NUM item
     * @param units        measurement units if NUM, otherwise {@code null}
     * @param depth        depth in the content tree (1 = top-level leaf)
     * @param valueType    raw {@code ValueType} (CODE / TEXT / NUM / DATE …)
     */
    public record Finding(
            String conceptName,
            String valueCode,
            String text,
            Double numericValue,
            String units,
            int depth,
            String valueType
    ) {}

    /**
     * Document-level metadata pulled from the SR header. Patient id is the
     * <b>raw</b> id from the SR — the caller pseudonymises before emitting
     * a signal (07-DICOM.md §6, §9 S9, §10 R1).
     */
    public record SrDocument(
            String modality,
            String rawPatientId,
            String rawPatientName,
            String bodyPart,
            String institution,
            List<Finding> findings
    ) {}

    private final List<String> supportedSchemes;

    public DicomSrParser() {
        this(List.of(SCHEME_SNOMED, SCHEME_LOINC, SCHEME_RADLEX, SCHEME_DCM));
    }

    public DicomSrParser(List<String> supportedSchemes) {
        this.supportedSchemes = List.copyOf(supportedSchemes);
    }

    /**
     * Parse a DICOM+JSON SR instance.
     *
     * @return the SR document metadata plus its leaf findings. The returned
     *         object is never {@code null}; missing fields surface as
     *         {@code null} / empty list.
     */
    public SrDocument parse(JsonNode srInstance) {
        if (srInstance == null || srInstance.isNull() || !srInstance.isObject()) {
            return new SrDocument(null, null, null, null, null, List.of());
        }
        String modality   = stringValue(srInstance, TAG_MODALITY);
        String patientId  = stringValue(srInstance, TAG_PATIENT_ID);
        String patientName = patientNameValue(srInstance, TAG_PATIENT_NAME);
        String bodyPart   = stringValue(srInstance, TAG_BODY_PART_EXAMINED);
        String institution = stringValue(srInstance, TAG_INSTITUTION_NAME);

        List<Finding> findings = new ArrayList<>();
        walk(srInstance, 0, findings);
        return new SrDocument(modality, patientId, patientName, bodyPart, institution, findings);
    }

    /** Quick category inference for a finding leaf. Conservative by default. */
    public FindingCategory inferCategory(Finding f) {
        if (f == null) return FindingCategory.INDETERMINATE;
        String code = f.valueCode == null ? null : f.valueCode.toUpperCase(Locale.ROOT);
        if (code != null) {
            // A few common SNOMED concept ids; everything else stays INDETERMINATE.
            if (code.endsWith(":17621005") || code.endsWith("-NORMAL") || code.equals("NORMAL")) {
                return FindingCategory.NORMAL;
            }
            if (code.endsWith(":263654008") || code.contains("INCIDENTAL")) {
                return FindingCategory.INCIDENTAL;
            }
            if (code.endsWith(":24484000") || code.contains("CRITICAL") || code.contains("URGENT")) {
                return FindingCategory.CRITICAL;
            }
            if (code.contains("ABNORMAL") || code.contains("MASS") || code.contains("LESION")
                    || code.contains("HEMORRHAGE") || code.contains("NODULE")) {
                return FindingCategory.ABNORMAL;
            }
        }
        if (f.text != null && f.text.toLowerCase(Locale.ROOT).contains("normal")) {
            return FindingCategory.NORMAL;
        }
        return FindingCategory.INDETERMINATE;
    }

    /** {@code true} iff {@code scheme} is one of the schemes the bridge claims to understand. */
    public boolean schemeSupported(String scheme) {
        if (scheme == null) return false;
        for (String s : supportedSchemes) {
            if (s.equalsIgnoreCase(scheme)) return true;
        }
        // OID-form acceptance (SNOMED, LOINC).
        return SCHEME_SNOMED_OID.equals(scheme) || SCHEME_LOINC_OID.equals(scheme);
    }

    /* ===== walk ========================================================= */

    private void walk(JsonNode node, int depth, List<Finding> out) {
        JsonNode contentSeq = sequence(node, TAG_CONTENT_SEQUENCE);
        if (contentSeq == null) return;
        for (Iterator<JsonNode> it = contentSeq.elements(); it.hasNext(); ) {
            JsonNode item = it.next();
            if (item == null || item.isNull()) continue;
            String valueType = stringValue(item, TAG_VALUE_TYPE);
            JsonNode children = sequence(item, TAG_CONTENT_SEQUENCE);
            boolean hasChildren = children != null && children.size() > 0;
            if ("CONTAINER".equalsIgnoreCase(valueType) || hasChildren) {
                walk(item, depth + 1, out);
                continue;
            }
            Finding f = leaf(item, valueType, depth + 1);
            if (f != null) out.add(f);
        }
    }

    private Finding leaf(JsonNode item, String valueType, int depth) {
        String conceptName = codedConcept(item, TAG_CONCEPT_NAME);
        if (valueType == null) return null;
        String type = valueType.toUpperCase(Locale.ROOT);
        switch (type) {
            case "CODE" -> {
                String valueCode = codedConcept(item, TAG_CONCEPT_CODE);
                return new Finding(conceptName, valueCode, null, null, null, depth, type);
            }
            case "TEXT" -> {
                String text = stringValue(item, TAG_TEXT_VALUE);
                if (text == null) return null;
                return new Finding(conceptName, null, text, null, null, depth, type);
            }
            case "NUM" -> {
                JsonNode mvs = sequence(item, TAG_MEASURED_VALUE_SEQ);
                if (mvs == null || mvs.size() == 0) return null;
                JsonNode mv = mvs.get(0);
                Double value = numericValue(mv, TAG_NUMERIC_VALUE);
                String units = codedConcept(mv, TAG_MEASUREMENT_UNITS);
                return new Finding(conceptName, null, null, value, units, depth, type);
            }
            default -> {
                String text = stringValue(item, TAG_TEXT_VALUE);
                return new Finding(conceptName, null, text, null, null, depth, type);
            }
        }
    }

    /* ===== JSON helpers ================================================= */

    private static JsonNode sequence(JsonNode node, String tag) {
        JsonNode attr = node.get(tag);
        if (attr == null || attr.isNull()) return null;
        JsonNode value = attr.get("Value");
        return (value != null && value.isArray()) ? value : null;
    }

    private static String stringValue(JsonNode node, String tag) {
        JsonNode attr = node.get(tag);
        if (attr == null || attr.isNull()) return null;
        JsonNode value = attr.get("Value");
        if (value == null || !value.isArray() || value.size() == 0) return null;
        JsonNode first = value.get(0);
        return first == null || first.isNull() ? null : first.asText();
    }

    private static Double numericValue(JsonNode node, String tag) {
        JsonNode attr = node.get(tag);
        if (attr == null || attr.isNull()) return null;
        JsonNode value = attr.get("Value");
        if (value == null || !value.isArray() || value.size() == 0) return null;
        JsonNode first = value.get(0);
        if (first == null || first.isNull()) return null;
        if (first.isNumber()) return first.asDouble();
        try {
            return Double.valueOf(first.asText());
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String patientNameValue(JsonNode node, String tag) {
        JsonNode attr = node.get(tag);
        if (attr == null || attr.isNull()) return null;
        JsonNode value = attr.get("Value");
        if (value == null || !value.isArray() || value.size() == 0) return null;
        JsonNode first = value.get(0);
        if (first == null || first.isNull()) return null;
        // DICOM PN VR — either {"Alphabetic":"DOE^JOHN"} or a plain string.
        if (first.isTextual()) return first.asText();
        JsonNode alpha = first.get("Alphabetic");
        return alpha == null ? null : alpha.asText();
    }

    /**
     * Render a CodeableConcept-style attribute as {@code "SCHEME:code"}.
     * Returns {@code null} if the attribute is missing or malformed.
     */
    private static String codedConcept(JsonNode node, String tag) {
        JsonNode attr = node.get(tag);
        if (attr == null || attr.isNull()) return null;
        JsonNode value = attr.get("Value");
        if (value == null || !value.isArray() || value.size() == 0) return null;
        JsonNode first = value.get(0);
        if (first == null || first.isNull()) return null;
        // SR coded entry: 00080100 CodeValue, 00080102 CodingSchemeDesignator,
        //                 00080104 CodeMeaning.
        String code = stringValue(first, "00080100");
        String scheme = stringValue(first, "00080102");
        String meaning = stringValue(first, "00080104");
        if (code == null && meaning != null) return meaning;
        if (code == null) return null;
        return (scheme == null ? "?" : scheme) + ":" + code;
    }
}
