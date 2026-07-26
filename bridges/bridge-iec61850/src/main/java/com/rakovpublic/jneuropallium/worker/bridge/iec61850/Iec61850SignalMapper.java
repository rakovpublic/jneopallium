/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.iec61850;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure-function mapping from IEC 61850 reads / reports to Jneopallium
 * signals (11-IEC61850.md §5).
 *
 * <p>Mapping table (verbatim from §5):
 * <ul>
 *   <li>{@code MMXU} measurements (PhV/PhA/W/Hz, ...) →
 *       {@link MeasurementSignal} with {@link Quality} from the {@code q}
 *       attribute.</li>
 *   <li>{@code XCBR.Pos} breaker position →
 *       {@link MeasurementSignal} with value {@code 1.0} for CLOSED and
 *       {@code 0.0} for OPEN. The 11-IEC61850.md §5 mapping names
 *       {@code InterlockSignal} for state semantics; the framework reserves
 *       that signal type for the aggregator's interlock-trip path
 *       (00-FRAMEWORK §0 rule 2), so a state observation from a breaker
 *       lives on the measurement channel — never as a directive.</li>
 *   <li>{@code PIOC}/{@code PTOC} operate signals →
 *       {@link AlarmSignal} with severity from the binding's
 *       {@code severityMap}. These are protection trips — read-only
 *       awareness, not commands.</li>
 *   <li>Other Logical Node classes default to {@link AlarmPriority#JOURNAL}.</li>
 * </ul>
 *
 * <p>Per 00-FRAMEWORK §0 rule 5, quality propagates unmodified: an
 * {@code INVALID} {@code q} attribute becomes {@link Quality#BAD};
 * {@code QUESTIONABLE} and {@code SUBSTITUTED} both become
 * {@link Quality#UNCERTAIN}. Untrustworthy data is never silently
 * promoted to "good".
 */
public final class Iec61850SignalMapper {

    /** Conservative default if a Logical Node class is not in the binding's severity map. */
    public static final AlarmPriority DEFAULT_ALARM_PRIORITY = AlarmPriority.JOURNAL;

    private Iec61850SignalMapper() {}

    /** §5 quality mapping. {@code null} maps to {@link Quality#UNCERTAIN}. */
    public static Quality mapQuality(Iec61850MmsClient.Iec61850Quality q) {
        if (q == null) return Quality.UNCERTAIN;
        return switch (q) {
            case GOOD -> Quality.GOOD;
            case INVALID -> Quality.BAD;
            case QUESTIONABLE, SUBSTITUTED -> Quality.UNCERTAIN;
        };
    }

    /**
     * Build the input signal a DA read produces, or {@code null} if the
     * read is absent (no current value reported by the IED).
     */
    public static IInputSignal mapDaRead(Iec61850DaBinding binding,
                                         Iec61850MmsClient.MmsRead read) {
        Objects.requireNonNull(binding, "binding");
        Objects.requireNonNull(read, "read");
        if (read.isAbsent()) return null;
        long ts = read.sourceTimestampMillis() != null
                ? read.sourceTimestampMillis() : System.currentTimeMillis();
        Quality quality = mapQuality(read.quality());

        return switch (binding.targetSignal()) {
            case MEASUREMENT -> new MeasurementSignal(
                    binding.signalTag(),
                    read.numericValue() != null ? read.numericValue()
                            : (Boolean.TRUE.equals(read.booleanValue()) ? 1.0 : 0.0),
                    quality, ts);
            case STATUS -> {
                boolean closed = Boolean.TRUE.equals(read.booleanValue());
                yield new MeasurementSignal(
                        binding.signalTag(),
                        closed ? 1.0 : 0.0,
                        quality, ts);
            }
            case ALARM -> new AlarmSignal(
                    DEFAULT_ALARM_PRIORITY, binding.signalTag(),
                    summariseDaPath(binding.daPath()), ts);
        };
    }

    /**
     * Build the alarm signals one Report Control Block report produces.
     * Severity is looked up per Logical Node class against the binding's
     * {@code severityMap} (11-IEC61850.md §6); entries whose LN class is
     * absent from the map fall through to {@link #DEFAULT_ALARM_PRIORITY}.
     */
    public static List<IInputSignal> mapReport(Iec61850BridgeConfig.ReportEventConfig event,
                                               Iec61850MmsClient.MmsReport report,
                                               String signalTagPrefix) {
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(report, "report");
        long ts = report.sourceTimestampMillis() > 0
                ? report.sourceTimestampMillis() : System.currentTimeMillis();
        List<IInputSignal> out = new ArrayList<>(report.entries().size());
        Map<String, String> sev = event.severityMap();
        String prefix = signalTagPrefix == null ? event.bindingId() : signalTagPrefix;
        for (Iec61850MmsClient.MmsReport.Entry entry : report.entries()) {
            AlarmPriority p = priorityFor(entry.logicalNodeClass(), sev);
            String tag = prefix + "." + entry.logicalNodeClass();
            String reason = entry.reason() == null
                    ? protectionConditionFor(entry.logicalNodeClass()) : entry.reason();
            out.add(new AlarmSignal(p, tag, reason, ts));
        }
        return out;
    }

    private static AlarmPriority priorityFor(String lnClass, Map<String, String> sev) {
        if (lnClass == null) return DEFAULT_ALARM_PRIORITY;
        String configured = sev.get(lnClass);
        if (configured == null) return DEFAULT_ALARM_PRIORITY;
        // §6 vocabulary: CRITICAL / HIGH / LOW / JOURNAL. CRITICAL maps to
        // URGENT in ISA-18.2 (the closest in-tree priority — there is no
        // CRITICAL value on AlarmPriority).
        return switch (configured.toUpperCase()) {
            case "CRITICAL", "URGENT" -> AlarmPriority.URGENT;
            case "HIGH" -> AlarmPriority.HIGH;
            case "LOW" -> AlarmPriority.LOW;
            default -> AlarmPriority.JOURNAL;
        };
    }

    /**
     * Default condition code for the protection-LN classes called out in
     * the spec (§5 — "PIOC/PTOC operate signals").
     */
    private static String protectionConditionFor(String lnClass) {
        if (lnClass == null) return "EVENT";
        return switch (lnClass) {
            case "PIOC", "PTOC", "PTUV", "PTOV", "PDIF" -> "PROTECTION_OPERATE";
            case "XCBR", "XSWI" -> "BREAKER_POSITION_CHANGE";
            default -> "EVENT";
        };
    }

    private static String summariseDaPath(String daPath) {
        int slash = daPath.indexOf('/');
        return slash < 0 ? daPath : daPath.substring(slash + 1);
    }
}
