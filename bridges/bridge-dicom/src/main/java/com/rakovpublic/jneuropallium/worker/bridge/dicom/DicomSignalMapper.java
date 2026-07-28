/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical.FindingCategory;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Converts parsed {@link DicomSrParser.SrDocument}s into Jneopallium
 * {@link ImagingFindingSignal}s (07-DICOM.md §5, §9 S7).
 *
 * <p>One signal is emitted per leaf finding. The mapper is responsible for:
 * <ul>
 *   <li>Pseudonymising the patient id (§9 S9, §10 R1) — the raw id never
 *       enters the emitted signal.</li>
 *   <li>Inferring {@link FindingCategory} via the parser's coding-scheme
 *       heuristics.</li>
 *   <li>Choosing a confidence value: numeric SR items derive confidence
 *       from the source coding scheme (supported → 0.85, unsupported →
 *       0.5 per §10 R3 "unknown codes pass through ... with
 *       Quality.UNCERTAIN").</li>
 * </ul>
 *
 * <p>The mapper never reads pixel data — the WADO request the bridge
 * issues is metadata-only and the SR is not a pixel object. §4 diagram:
 * "Image bytes never enter the JVM heap."
 */
public final class DicomSignalMapper {

    private final DicomPseudonymService pseudonyms;
    private final DicomSrParser parser;
    private final boolean redactPatientName;

    public DicomSignalMapper(DicomPseudonymService pseudonyms,
                             DicomSrParser parser,
                             boolean redactPatientName) {
        this.pseudonyms = Objects.requireNonNull(pseudonyms, "pseudonyms");
        this.parser = Objects.requireNonNull(parser, "parser");
        this.redactPatientName = redactPatientName;
    }

    /**
     * Build one {@link ImagingFindingSignal} per leaf finding in
     * {@code doc}. The returned list is never {@code null} and never
     * contains a signal carrying the raw patient id.
     */
    public List<ImagingFindingSignal> mapDocument(DicomSrParser.SrDocument doc) {
        if (doc == null || doc.findings().isEmpty()) return List.of();
        String pseudoPid = pseudonyms.pseudonymise(doc.rawPatientId());
        // S9 — patient name is dropped on the signal regardless; the redaction
        // flag governs whether it makes it to the audit metadata at all.
        if (redactPatientName) {
            // No-op for the signal: ImagingFindingSignal has no patient-name field.
            // The flag controls the audit path (see DicomClientService).
        }
        String modality = doc.modality();
        String region = doc.bodyPart();
        List<ImagingFindingSignal> out = new ArrayList<>(doc.findings().size());
        for (DicomSrParser.Finding f : doc.findings()) {
            FindingCategory category = parser.inferCategory(f);
            double confidence = inferConfidence(f);
            out.add(new ImagingFindingSignal(modality, region, category, confidence, pseudoPid));
        }
        return out;
    }

    private double inferConfidence(DicomSrParser.Finding f) {
        // §10 R3 — unknown codes pass through as opaque strings with Quality.UNCERTAIN.
        // We map "unknown scheme" to a low (0.5) confidence; supported schemes get 0.85.
        if (f == null || f.valueCode() == null) return 0.6;
        int colon = f.valueCode().indexOf(':');
        String scheme = colon < 0 ? null : f.valueCode().substring(0, colon);
        return parser.schemeSupported(scheme) ? 0.85 : 0.5;
    }
}
