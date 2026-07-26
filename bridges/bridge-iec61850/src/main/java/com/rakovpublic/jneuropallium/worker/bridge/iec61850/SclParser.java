/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal SCL (Substation Configuration Language) parser
 * (11-IEC61850.md §7 — {@code SclParser.java}).
 *
 * <p>SCL is the ground-truth data model for an IED — parsing it lets the
 * bridge resolve Data Attribute paths and Logical Node classes without
 * relying on runtime introspection of the device. This parser is
 * intentionally narrow: it extracts the
 * {@code IED → AccessPoint → Server → LDevice → LN → DO → DA} skeleton
 * needed to validate read bindings and identify Logical Node classes
 * (so the bridge can map an {@code PIOC} report entry to the right
 * severity per §5).
 *
 * <p>SCL dialect drift between vendors is mitigated by:
 * <ul>
 *   <li>Ignoring namespaces — the parser is element-name driven.</li>
 *   <li>Ignoring attributes the bridge does not use (data types, addresses,
 *       services). This keeps the parser stable across ABB, Schneider, SEL
 *       and other vendor stacks (§10 R3).</li>
 * </ul>
 *
 * <p>The bridge <b>fails fast at startup</b> if the SCL file is missing or
 * malformed (§9 S9; §6 last line).
 */
public final class SclParser {

    private SclParser() {}

    /** Parse an SCL file by path. Throws {@link IOException} if the file is missing. */
    public static SclModel parseFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException(
                    "IEC 61850 bridge: SCL file not found at " + file
                            + " (11-IEC61850.md §6 — SCL is required ground-truth; "
                            + "the bridge fails fast at startup).");
        }
        try (InputStream in = Files.newInputStream(file)) {
            return parse(in);
        }
    }

    /** Parse an SCL stream. */
    public static SclModel parse(InputStream in) throws IOException {
        Objects.requireNonNull(in, "in");
        XMLInputFactory factory = XMLInputFactory.newFactory();
        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);

        List<Ied> ieds = new ArrayList<>();
        Ied currentIed = null;
        LDevice currentLd = null;
        LNode currentLn = null;
        DataObject currentDo = null;

        try {
            XMLStreamReader r = factory.createXMLStreamReader(in);
            while (r.hasNext()) {
                int event = r.next();
                if (event == XMLStreamReader.START_ELEMENT) {
                    String name = localName(r.getLocalName());
                    switch (name) {
                        case "IED" -> currentIed = new Ied(attr(r, "name"));
                        case "LDevice" -> {
                            if (currentIed != null) {
                                currentLd = new LDevice(attr(r, "inst"));
                                currentIed.devices.add(currentLd);
                            }
                        }
                        case "LN0", "LN" -> {
                            if (currentLd != null) {
                                String lnClass = attr(r, "lnClass");
                                String prefix = attr(r, "prefix");
                                String inst = attr(r, "inst");
                                currentLn = new LNode(lnClass, prefix, inst);
                                currentLd.nodes.add(currentLn);
                            }
                        }
                        case "DOI" -> {
                            if (currentLn != null) {
                                currentDo = new DataObject(attr(r, "name"));
                                currentLn.dataObjects.add(currentDo);
                            }
                        }
                        case "DAI" -> {
                            if (currentDo != null) {
                                currentDo.attributes.add(new DataAttribute(
                                        attr(r, "name"), attr(r, "valKind")));
                            }
                        }
                        default -> { /* ignore */ }
                    }
                } else if (event == XMLStreamReader.END_ELEMENT) {
                    String name = localName(r.getLocalName());
                    switch (name) {
                        case "IED" -> { if (currentIed != null) { ieds.add(currentIed); currentIed = null; } }
                        case "LDevice" -> currentLd = null;
                        case "LN0", "LN" -> currentLn = null;
                        case "DOI" -> currentDo = null;
                        default -> { /* ignore */ }
                    }
                }
            }
            r.close();
        } catch (XMLStreamException e) {
            throw new IOException(
                    "IEC 61850 bridge: malformed SCL — " + e.getMessage()
                            + " (11-IEC61850.md §6 — bridge fails fast at startup "
                            + "if SCL is missing or malformed).", e);
        }
        return new SclModel(ieds);
    }

    private static String localName(String n) {
        int idx = n.indexOf(':');
        return idx < 0 ? n : n.substring(idx + 1);
    }

    private static String attr(XMLStreamReader r, String name) {
        for (int i = 0; i < r.getAttributeCount(); i++) {
            if (name.equals(r.getAttributeLocalName(i))) return r.getAttributeValue(i);
        }
        return null;
    }

    /** Parsed SCL document, narrowed to the fields the bridge uses. */
    public static final class SclModel {
        private final List<Ied> ieds;

        SclModel(List<Ied> ieds) {
            this.ieds = List.copyOf(ieds);
        }

        public List<Ied> ieds() { return ieds; }

        public Ied iedByName(String name) {
            for (Ied i : ieds) if (name.equals(i.name())) return i;
            return null;
        }

        /**
         * Find the Logical Node class for a Data Attribute path of the form
         * {@code "<LD>/<LN>.<DO>.<DA>..."} where {@code <LN>} may include a
         * prefix and instance suffix. Returns {@code null} if the path
         * cannot be resolved against this SCL.
         */
        public String resolveLogicalNodeClass(String daPath) {
            if (daPath == null) return null;
            int slash = daPath.indexOf('/');
            if (slash < 0) return null;
            String ldInst = daPath.substring(0, slash);
            String tail = daPath.substring(slash + 1);
            int dot = tail.indexOf('.');
            String lnToken = dot < 0 ? tail : tail.substring(0, dot);
            for (Ied ied : ieds) {
                for (LDevice ld : ied.devices) {
                    if (ldInst.equals(ld.inst())) {
                        for (LNode ln : ld.nodes) {
                            if (lnToken.equals(ln.combinedName())) return ln.lnClass();
                        }
                        for (LNode ln : ld.nodes) {
                            if (ln.lnClass() != null && lnToken.startsWith(ln.lnClass())) {
                                return ln.lnClass();
                            }
                        }
                    }
                }
            }
            return null;
        }

        /** Distinct list of Logical Node classes present in the SCL. */
        public List<String> logicalNodeClasses() {
            Map<String, Boolean> seen = new LinkedHashMap<>();
            for (Ied ied : ieds) {
                for (LDevice ld : ied.devices) {
                    for (LNode ln : ld.nodes) {
                        if (ln.lnClass() != null) seen.put(ln.lnClass(), Boolean.TRUE);
                    }
                }
            }
            return new ArrayList<>(seen.keySet());
        }
    }

    /** IED element in SCL. */
    public static final class Ied {
        private final String name;
        private final List<LDevice> devices = new ArrayList<>();
        Ied(String name) { this.name = name; }
        public String name() { return name; }
        public List<LDevice> devices() { return List.copyOf(devices); }
    }

    /** Logical Device element. */
    public static final class LDevice {
        private final String inst;
        private final List<LNode> nodes = new ArrayList<>();
        LDevice(String inst) { this.inst = inst; }
        public String inst() { return inst; }
        public List<LNode> nodes() { return List.copyOf(nodes); }
    }

    /** Logical Node element ({@code LN0}/{@code LN}). */
    public static final class LNode {
        private final String lnClass;
        private final String prefix;
        private final String inst;
        private final List<DataObject> dataObjects = new ArrayList<>();
        LNode(String lnClass, String prefix, String inst) {
            this.lnClass = lnClass;
            this.prefix = prefix;
            this.inst = inst;
        }
        public String lnClass() { return lnClass; }
        public String prefix() { return prefix; }
        public String inst() { return inst; }
        public List<DataObject> dataObjects() { return List.copyOf(dataObjects); }

        /** Canonical {@code <prefix><lnClass><inst>} token used in DA paths. */
        public String combinedName() {
            StringBuilder sb = new StringBuilder();
            if (prefix != null) sb.append(prefix);
            if (lnClass != null) sb.append(lnClass);
            if (inst != null) sb.append(inst);
            return sb.toString();
        }
    }

    /** Data Object element. */
    public static final class DataObject {
        private final String name;
        private final List<DataAttribute> attributes = new ArrayList<>();
        DataObject(String name) { this.name = name; }
        public String name() { return name; }
        public List<DataAttribute> attributes() { return List.copyOf(attributes); }
    }

    /** Data Attribute leaf. */
    public record DataAttribute(String name, String valKind) {}
}
