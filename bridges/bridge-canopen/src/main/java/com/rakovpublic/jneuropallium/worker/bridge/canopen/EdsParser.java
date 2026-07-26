/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Permissive Electronic Data Sheet parser (13-CANOPEN.md §6, §10 R2).
 *
 * <p>EDS / DCF files are INI-style (CiA-306). This parser reads the
 * subset that the bridge actually needs for OD-aware PDO decoding:
 * sections matching {@code [hhhh]} (object) or {@code [hhhhsubXX]}
 * (sub-object) and the {@code DataType} / {@code AccessType} /
 * {@code ParameterName} / {@code DefaultValue} keys. It is permissive by
 * design (§10 R2): unknown sections / keys are logged as warnings rather
 * than rejected, so the parser tolerates EDS dialect drift between
 * vendors.
 *
 * <p>The result is a flat {@code Map<Integer key, ObjectDictionaryEntry>}
 * keyed by {@code (index << 8) | subIndex}.
 */
public final class EdsParser {

    private static final Logger log = LoggerFactory.getLogger(EdsParser.class);

    /** Section header: {@code [6041]} or {@code [6041sub1]} (case-insensitive). */
    private static final Pattern SECTION_RE = Pattern.compile(
            "^\\[\\s*([0-9A-Fa-f]{1,4})(?:\\s*sub\\s*([0-9A-Fa-f]{1,2}))?\\s*\\]\\s*$");

    private static final Pattern KV_RE = Pattern.compile(
            "^\\s*([A-Za-z_][A-Za-z0-9_]*)\\s*=\\s*(.*?)\\s*$");

    private EdsParser() {}

    public static Map<Integer, ObjectDictionaryEntry> parse(Path file) throws IOException {
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parse(r);
        }
    }

    public static Map<Integer, ObjectDictionaryEntry> parse(String content) throws IOException {
        try (BufferedReader r = new BufferedReader(new StringReader(content))) {
            return parse(r);
        }
    }

    public static Map<Integer, ObjectDictionaryEntry> parse(Reader reader) throws IOException {
        BufferedReader br = (reader instanceof BufferedReader b) ? b : new BufferedReader(reader);
        Map<Integer, ObjectDictionaryEntry> out = new LinkedHashMap<>();
        Map<String, String> sectionKv = new LinkedHashMap<>();
        Integer currentIndex = null;
        Integer currentSub = null;

        String line;
        while ((line = br.readLine()) != null) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#")) {
                continue;
            }
            Matcher sec = SECTION_RE.matcher(trimmed);
            if (sec.matches()) {
                flush(out, currentIndex, currentSub, sectionKv);
                sectionKv = new LinkedHashMap<>();
                currentIndex = Integer.parseInt(sec.group(1), 16);
                currentSub = sec.group(2) == null ? 0 : Integer.parseInt(sec.group(2), 16);
                continue;
            }
            if (line.startsWith("[")) {
                // A non-OD section (e.g. [FileInfo], [DeviceInfo]) — skip until next.
                flush(out, currentIndex, currentSub, sectionKv);
                sectionKv = new LinkedHashMap<>();
                currentIndex = null;
                currentSub = null;
                continue;
            }
            Matcher kv = KV_RE.matcher(line);
            if (kv.matches() && currentIndex != null) {
                sectionKv.put(kv.group(1), kv.group(2));
            }
        }
        flush(out, currentIndex, currentSub, sectionKv);
        return out;
    }

    private static void flush(Map<Integer, ObjectDictionaryEntry> out,
                              Integer index, Integer sub, Map<String, String> kv) {
        if (index == null || kv.isEmpty()) return;
        String dataTypeStr = kv.get("DataType");
        if (dataTypeStr == null) {
            // No DataType — likely a structure header (ParameterName + ObjectType only); skip.
            return;
        }
        CanopenBridgeConfig.OdType type = mapDataType(dataTypeStr);
        if (type == null) {
            log.debug("EDS: unsupported DataType {} at index 0x{}sub{} — skipping",
                    dataTypeStr, Integer.toHexString(index), sub);
            return;
        }
        ObjectDictionaryEntry.OdAccess access = mapAccess(kv.get("AccessType"));
        String name = kv.getOrDefault("ParameterName", "");
        String def = kv.get("DefaultValue");

        int key = (index << 8) | (sub & 0xff);
        out.put(key, new ObjectDictionaryEntry(index, sub, name, type, access, def));
    }

    /**
     * CiA-301 §7.4.7 data-type IDs. Permissive: anything we don't model
     * is silently skipped (the bridge falls back to the binding's
     * configured {@code odType}).
     */
    static CanopenBridgeConfig.OdType mapDataType(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int v;
        try {
            if (s.startsWith("0x") || s.startsWith("0X")) v = Integer.parseInt(s.substring(2), 16);
            else v = Integer.parseInt(s, 16);
        } catch (NumberFormatException nfe) {
            return null;
        }
        return switch (v) {
            case 0x0002 -> CanopenBridgeConfig.OdType.INT8;
            case 0x0003 -> CanopenBridgeConfig.OdType.INT16;
            case 0x0004 -> CanopenBridgeConfig.OdType.INT32;
            case 0x0005 -> CanopenBridgeConfig.OdType.UINT8;
            case 0x0006 -> CanopenBridgeConfig.OdType.UINT16;
            case 0x0007 -> CanopenBridgeConfig.OdType.UINT32;
            case 0x0008 -> CanopenBridgeConfig.OdType.REAL32;
            default -> null;
        };
    }

    static ObjectDictionaryEntry.OdAccess mapAccess(String raw) {
        if (raw == null) return ObjectDictionaryEntry.OdAccess.RW;
        return switch (raw.trim().toLowerCase()) {
            case "ro" -> ObjectDictionaryEntry.OdAccess.RO;
            case "wo" -> ObjectDictionaryEntry.OdAccess.WO;
            case "rwr" -> ObjectDictionaryEntry.OdAccess.RWR;
            case "rww" -> ObjectDictionaryEntry.OdAccess.RWW;
            case "const" -> ObjectDictionaryEntry.OdAccess.CONST;
            default -> ObjectDictionaryEntry.OdAccess.RW;
        };
    }
}
