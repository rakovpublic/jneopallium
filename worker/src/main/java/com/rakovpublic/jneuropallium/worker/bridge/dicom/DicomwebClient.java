/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Thin DICOMweb query layer (07-DICOM.md §4, §7).
 *
 * <p>Drives the {@link DicomwebTransport} seam — one QIDO call per binding
 * to enumerate matching studies, then a WADO-RS metadata fetch per
 * candidate SR instance. Per §10 R1, the WADO call asks for metadata only
 * (the transport pins the {@code Accept} header at
 * {@code application/dicom+json}).
 *
 * <p>This client returns parsed JSON; the {@link DicomSrParser} consumes
 * it and the {@link DicomSignalMapper} turns the parsed findings into
 * signals. Splitting the layers like this lets the bridge run against the
 * Orthanc demo with one wire transport and against the test fixtures with
 * a different one — the parser is identical.
 */
public final class DicomwebClient {

    private final DicomwebTransport transport;
    private final DicomBridgeConfig config;

    public DicomwebClient(DicomwebTransport transport, DicomBridgeConfig config) {
        this.transport = Objects.requireNonNull(transport, "transport");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Enumerate study summaries matching the binding's filter via QIDO-RS.
     * Each entry is a DICOM+JSON study attribute object.
     */
    public List<JsonNode> qidoStudies(DicomStudyBinding binding) throws IOException {
        DicomBridgeConfig.DicomwebConfig dw = config.dicomweb();
        if (dw == null) return List.of();
        String path = stripTrailingSlash(dw.qidoEndpoint()) + binding.qidoQueryString();
        JsonNode response = transport.qido(path);
        return toList(response);
    }

    /**
     * Fetch the WADO-RS metadata for an instance identified by its
     * Study/Series/SOPInstance UIDs (DICOM tags {@code 0020000D},
     * {@code 0020000E}, {@code 00080018}).
     */
    public List<JsonNode> wadoInstanceMetadata(String studyUid, String seriesUid, String instanceUid)
            throws IOException {
        DicomBridgeConfig.DicomwebConfig dw = config.dicomweb();
        if (dw == null) return List.of();
        String path = dw.wadoEndpoint()
                .replace("{study}", studyUid)
                .replace("{series}", seriesUid)
                .replace("{instance}", instanceUid);
        JsonNode response = transport.wadoMetadata(path);
        return toList(response);
    }

    /** Convenience — given a QIDO study entry, fetch SR instances inside it. */
    public List<JsonNode> wadoSrInstancesForStudy(JsonNode studyAttrs) throws IOException {
        if (studyAttrs == null) return List.of();
        String studyUid = dicomTagFirst(studyAttrs, "0020000D");
        if (studyUid == null) return List.of();
        // For brevity the v1 bridge expects the QIDO response to already include
        // SeriesInstanceUID and SOPInstanceUID (modern PACSes return this when
        // includefield=all). Where it doesn't, the caller wires the per-series
        // QIDO + per-instance metadata calls explicitly.
        String seriesUid = dicomTagFirst(studyAttrs, "0020000E");
        String instanceUid = dicomTagFirst(studyAttrs, "00080018");
        if (seriesUid == null || instanceUid == null) return List.of();
        return wadoInstanceMetadata(studyUid, seriesUid, instanceUid);
    }

    /** True if the transport reports authenticated + ready. */
    public boolean isReady() { return transport.isReady(); }

    public DicomwebTransport transport() { return transport; }

    private static List<JsonNode> toList(JsonNode response) {
        List<JsonNode> out = new ArrayList<>();
        if (response == null || response.isNull()) return out;
        if (response.isArray()) {
            for (Iterator<JsonNode> it = response.elements(); it.hasNext(); ) {
                JsonNode n = it.next();
                if (n != null && !n.isNull()) out.add(n);
            }
            return out;
        }
        // Some servers return a single object — treat it as a list of one.
        out.add(response);
        return out;
    }

    private static String dicomTagFirst(JsonNode node, String tag) {
        JsonNode attr = node.get(tag);
        if (attr == null || attr.isNull()) return null;
        JsonNode value = attr.get("Value");
        if (value == null || !value.isArray() || value.size() == 0) return null;
        JsonNode first = value.get(0);
        return first == null || first.isNull() ? null : first.asText();
    }

    private static String stripTrailingSlash(String s) {
        if (s == null) return "/studies";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
