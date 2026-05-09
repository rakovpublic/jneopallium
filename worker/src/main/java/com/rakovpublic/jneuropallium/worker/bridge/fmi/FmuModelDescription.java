/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Parsed view of {@code modelDescription.xml} from inside an FMU ZIP.
 *
 * <p>Supports both FMI 2.0 (ScalarVariable elements) and FMI 3.0 (typed
 * variable elements: Float64Variable, BooleanVariable, Int32Variable).
 * Variable name → valueReference mappings are used by {@link FmuClientService}
 * to translate the YAML-configured variable names into the integer references
 * expected by {@link FmuDriver}.
 */
public final class FmuModelDescription {

    private static final Logger log = LoggerFactory.getLogger(FmuModelDescription.class);

    private final String modelName;
    private final String fmiVersion;
    private final Map<String, Integer> nameToRef;

    private FmuModelDescription(String modelName, String fmiVersion, Map<String, Integer> nameToRef) {
        this.modelName = Objects.requireNonNull(modelName);
        this.fmiVersion = Objects.requireNonNull(fmiVersion);
        this.nameToRef = Collections.unmodifiableMap(new HashMap<>(nameToRef));
    }

    /**
     * Parse a modelDescription.xml stream. Never throws; returns a partial
     * description (logged as WARN) on parse failure.
     */
    public static FmuModelDescription parse(InputStream xml) {
        Objects.requireNonNull(xml, "xml");
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document doc = dbf.newDocumentBuilder().parse(xml);
            Element root = doc.getDocumentElement();

            String modelName = root.getAttribute("modelName");
            String fmiVersion = root.getAttribute("fmiVersion");
            Map<String, Integer> nameToRef = new HashMap<>();

            NodeList fmi2Vars = root.getElementsByTagName("ScalarVariable");
            if (fmi2Vars.getLength() > 0) {
                // FMI 2.0: each ScalarVariable has name + valueReference attributes
                for (int i = 0; i < fmi2Vars.getLength(); i++) {
                    collectVar((Element) fmi2Vars.item(i), nameToRef);
                }
            } else {
                // FMI 3.0: typed variable elements
                for (String tag : new String[]{
                        "Float64Variable", "Float32Variable",
                        "BooleanVariable", "Int32Variable", "Int64Variable",
                        "StringVariable", "BinaryVariable", "EnumerationVariable"
                }) {
                    NodeList vars = doc.getElementsByTagName(tag);
                    for (int i = 0; i < vars.getLength(); i++) {
                        collectVar((Element) vars.item(i), nameToRef);
                    }
                }
            }
            return new FmuModelDescription(
                    modelName.isEmpty() ? "unknown" : modelName,
                    fmiVersion.isEmpty() ? "2.0" : fmiVersion,
                    nameToRef);
        } catch (Exception e) {
            log.warn("Failed to parse modelDescription.xml: {}; using empty description", e.getMessage());
            return new FmuModelDescription("unknown", "2.0", new HashMap<>());
        }
    }

    /**
     * Construct a description programmatically (e.g. for stub drivers in tests).
     *
     * @param nameToRef variable name → valueReference map
     */
    public static FmuModelDescription of(String modelName, String fmiVersion,
                                         Map<String, Integer> nameToRef) {
        return new FmuModelDescription(modelName, fmiVersion, nameToRef);
    }

    /** Resolve variable name to its FMI valueReference. */
    public int valueReference(String variableName) {
        Integer vr = nameToRef.get(variableName);
        if (vr == null) {
            throw new FmuException("Unknown FMU variable: '" + variableName + "'. "
                    + "Available: " + nameToRef.keySet());
        }
        return vr;
    }

    public boolean contains(String variableName) {
        return nameToRef.containsKey(variableName);
    }

    public String modelName() { return modelName; }
    public String fmiVersion() { return fmiVersion; }
    public Map<String, Integer> nameToRef() { return nameToRef; }

    private static void collectVar(Element el, Map<String, Integer> out) {
        String name = el.getAttribute("name");
        String vr = el.getAttribute("valueReference");
        if (!name.isEmpty() && !vr.isEmpty()) {
            try {
                out.put(name, Integer.parseInt(vr));
            } catch (NumberFormatException ignore) {
                log.warn("Non-integer valueReference '{}' for variable '{}'; skipped", vr, name);
            }
        }
    }
}
