/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBinding;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeBindingDirection;

import java.util.Objects;

/**
 * Resolved per-binding DICOM study query (07-DICOM.md §6, §7).
 *
 * <p>Every binding is {@link BridgeBindingDirection#READ} — the DICOM
 * bridge has no write direction (§3, §0). The clamp / ramp / loop fields
 * required by {@link BridgeBinding} are therefore {@code null}; they exist
 * on the interface so the universal §2.2 audit path treats a DICOM study
 * query uniformly with industrial bindings, but the DICOM bridge never
 * invokes them (no write code path exists).
 */
public record DicomStudyBinding(
        String bindingId,
        DicomBridgeConfig.StudyFilterConfig studyFilter,
        DicomBridgeConfig.TargetSignal targetSignal,
        String signalTagPrefix
) implements BridgeBinding {

    public DicomStudyBinding {
        Objects.requireNonNull(bindingId, "bindingId");
        if (targetSignal == null) targetSignal = DicomBridgeConfig.TargetSignal.IMAGING_FINDING;
    }

    public static DicomStudyBinding fromConfig(DicomBridgeConfig.StudyReadConfig r) {
        return new DicomStudyBinding(
                r.bindingId(),
                r.studyFilter(),
                r.targetSignal(),
                r.signalTagPrefix());
    }

    @Override public BridgeBindingDirection direction() { return BridgeBindingDirection.READ; }
    @Override public String loopId() { return bindingId; }
    @Override public String signalTag() { return signalTagPrefix; }
    @Override public Double failSafeValue() { return null; }
    @Override public Double rampRateMaxPerSec() { return null; }
    @Override public Double minClampValue() { return null; }
    @Override public Double maxClampValue() { return null; }

    /** Modality filter (e.g. {@code "SR"}). Returns {@code null} if not configured. */
    public String modality() {
        return studyFilter == null ? null : studyFilter.modality();
    }

    /** Accession number glob (e.g. {@code "RAD-*"}). Returns {@code null} if not configured. */
    public String accessionPattern() {
        return studyFilter == null ? null : studyFilter.accessionPattern();
    }

    /** Sliding window in hours; {@code null} means no time bound. */
    public Integer windowHours() {
        return studyFilter == null ? null : studyFilter.windowHours();
    }

    /**
     * Build a QIDO-RS query string (relative to {@code /studies}) representing
     * this binding's study filter. Used by {@link DicomwebClient} on every
     * poll cycle.
     */
    public String qidoQueryString() {
        StringBuilder qs = new StringBuilder();
        if (modality() != null) appendParam(qs, "ModalitiesInStudy", modality());
        if (accessionPattern() != null) appendParam(qs, "AccessionNumber", accessionPattern());
        if (windowHours() != null && windowHours() > 0) {
            appendParam(qs, "StudyDate", windowParam(windowHours()));
        }
        return qs.toString();
    }

    private static void appendParam(StringBuilder qs, String key, String value) {
        qs.append(qs.length() == 0 ? "?" : "&").append(key).append('=').append(value);
    }

    private static String windowParam(int hours) {
        // DICOM StudyDate is a date range; for the QIDO call we approximate
        // "last N hours" with the day-range starting at today minus the
        // ceiling-of (N/24) days. PACS servers vary in how strictly they
        // honour finer windowing — daily granularity is the safe default.
        int days = Math.max(1, (hours + 23) / 24);
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.LocalDate from = today.minusDays(days);
        return fmt(from) + "-" + fmt(today);
    }

    private static String fmt(java.time.LocalDate d) {
        return String.format("%04d%02d%02d", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    }
}
