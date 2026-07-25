/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Read-only MMS transport seam (11-IEC61850.md §4).
 *
 * <p>The bridge talks to IEDs only through this interface. No method
 * writes a Data Attribute, issues a select-before-operate, or controls a
 * breaker — substation control writes are a SIL-classified function that
 * lives on certified RTUs and protection relays (§3). A production
 * implementation backed by {@code org.openmuc:iec61850bean} or the JNI
 * bindings of {@code libiec61850} provides only the methods on this
 * surface; an attempt to add a write method here is a build break.
 *
 * <p>Acceptance tests use {@link InMemoryIec61850MmsClient}.
 */
public interface Iec61850MmsClient extends AutoCloseable {

    /** {@code true} when the underlying MMS association is ready for reads. */
    boolean isReady();

    /**
     * Read one Data Attribute by its IEC 61850 functional-constraint path
     * (e.g. {@code "LD0/MMXU1.PhV.phsA.cVal.mag.f"}). Implementations
     * return a snapshot of the latest value, optionally with a
     * vendor-provided quality and timestamp; if the IED has not reported
     * a value yet, the result MUST be {@link MmsRead#none()}.
     *
     * @throws IOException on association failure — caller decides whether
     *                     to enter the reconnect loop.
     */
    MmsRead readDa(String iedId, String daPath) throws IOException;

    /**
     * Subscribe to a Report Control Block on an IED. The {@code consumer}
     * is invoked once per buffered or unbuffered report from the IED.
     * Implementations MUST decouple the consumer from any network thread
     * — runtime exceptions thrown by the consumer must be isolated.
     *
     * @return an {@link AutoCloseable} that cancels the subscription;
     *         closing twice is a no-op.
     */
    AutoCloseable subscribeReport(String iedId, String reportControlBlock,
                                  Consumer<MmsReport> consumer) throws IOException;

    @Override
    void close();

    /**
     * Quality flag taken from the IEC 61850 {@code q} attribute. The
     * mapping to {@link com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality}
     * is documented in 11-IEC61850.md §5: {@code GOOD→GOOD},
     * {@code INVALID→BAD}, {@code QUESTIONABLE→UNCERTAIN},
     * {@code SUBSTITUTED→UNCERTAIN}.
     */
    enum Iec61850Quality { GOOD, INVALID, QUESTIONABLE, SUBSTITUTED }

    /** Snapshot of a Data Attribute read. Immutable; nulls allowed. */
    record MmsRead(Double numericValue, Boolean booleanValue,
                   Iec61850Quality quality, Long sourceTimestampMillis) {

        public static MmsRead none() {
            return new MmsRead(null, null, null, null);
        }

        public static MmsRead measurement(double value, Iec61850Quality q, long ts) {
            return new MmsRead(value, null, q == null ? Iec61850Quality.GOOD : q, ts);
        }

        public static MmsRead status(boolean closed, Iec61850Quality q, long ts) {
            return new MmsRead(null, closed, q == null ? Iec61850Quality.GOOD : q, ts);
        }

        public boolean isAbsent() {
            return numericValue == null && booleanValue == null;
        }
    }

    /**
     * One report from a Report Control Block (RP/BR). Contains one
     * {@link Entry} per Data Set member flagged by the report's data-set
     * reasons. Used for protection-trip awareness and status changes.
     */
    record MmsReport(String reportControlBlock,
                     long sourceTimestampMillis,
                     List<Entry> entries) {

        public MmsReport {
            entries = entries == null ? List.of() : List.copyOf(entries);
        }

        /**
         * One Data Set member in a report. {@code logicalNodeClass} is the
         * 4-letter IEC 61850 LN class (e.g. {@code "PIOC"}, {@code "PTOC"})
         * used to look up the severity in
         * {@link Iec61850BridgeConfig.ReportEventConfig#severityMap()}.
         */
        public record Entry(String daPath,
                            String logicalNodeClass,
                            Double numericValue,
                            Boolean booleanValue,
                            Iec61850Quality quality,
                            String reason) {
            public Entry {
                // null reason is fine — IEC 61850 reports do not always carry one.
            }
        }
    }
}
