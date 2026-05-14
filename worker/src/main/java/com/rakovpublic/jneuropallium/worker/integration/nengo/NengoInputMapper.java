/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.ai.signals.fast.SensorySignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.Quality;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.EfficiencySignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.MeasurementSignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Routes labeled frame values into typed Jneopallium signals
 * (15-NENGO.md §6.2).
 *
 * <p>The mapper is data-driven by the YAML {@code inputMappings} list.
 * Any frame label not bound in YAML falls back to {@link MeasurementSignal}
 * with {@code tag=label} — so a new sensor channel can be added on the
 * Python side without a Java recompile, at the cost of being typed as a
 * generic measurement until a YAML binding promotes it.
 *
 * <p>The frame's {@code safety_status} field maps to {@link Quality} on
 * derived industrial signals: {@code OK} → {@code GOOD},
 * {@code DEGRADED} → {@code UNCERTAIN}, {@code STOP} → {@code BAD}.
 * AI signals carry no quality value, but the per-frame status is
 * preserved in {@link NengoDecodedStateSignal#getSafetyStatus()} for the
 * harm gate to inspect.
 *
 * <p><b>HARM_ASSESSMENT note:</b> the framework's {@code readSignals()}
 * accepts {@link IInputSignal} only, while
 * {@link com.rakovpublic.jneuropallium.ai.signals.fast.HarmAssessmentSignal}
 * is a result signal produced internally by the harm-assessment neuron
 * layer. So a {@code HARM_ASSESSMENT}-typed binding is surfaced here as a
 * {@link MeasurementSignal} with the configured {@code signalTag}; a
 * downstream signal-processor or harm-assessment neuron can promote that
 * measurement into a typed {@code HarmAssessmentSignal} for the harm
 * gate. This preserves the data path without violating the
 * input/result-signal split.
 */
public final class NengoInputMapper {

    private final Map<String, NengoBridgeConfig.InputMapping> byLabel;

    public NengoInputMapper(List<NengoBridgeConfig.InputMapping> mappings) {
        Objects.requireNonNull(mappings, "mappings");
        Map<String, NengoBridgeConfig.InputMapping> idx = new LinkedHashMap<>();
        for (NengoBridgeConfig.InputMapping m : mappings) idx.put(m.frameLabel(), m);
        this.byLabel = Map.copyOf(idx);
    }

    /** §6.2 — fan {@code decoded} out into typed signals. */
    public List<IInputSignal> fanOut(NengoDecodedStateSignal decoded, String inputName) {
        Objects.requireNonNull(decoded, "decoded");
        List<IInputSignal> out = new ArrayList<>();
        Quality quality = qualityFor(decoded.getSafetyStatus());
        long ts = decoded.getTimestampMs();

        for (Map.Entry<String, Double> e : decoded.getValues().entrySet()) {
            String label = e.getKey();
            double v = e.getValue();
            NengoBridgeConfig.InputMapping m = byLabel.get(label);
            IInputSignal sig = (m == null)
                    ? defaultMeasurement(label, v, quality, ts)
                    : map(m, v, quality, ts);
            if (sig == null) continue;
            sig.setInputName(inputName);
            sig.setFromExternalNet(true);
            out.add(sig);
        }
        return out;
    }

    private IInputSignal map(NengoBridgeConfig.InputMapping m,
                             double v, Quality quality, long ts) {
        return switch (m.signal()) {
            case SENSORY -> {
                SensorySignal s = new SensorySignal(new double[]{v}, m.signalTag(), ts);
                yield s;
            }
            case HARM_ASSESSMENT ->
                    // See class Javadoc — HarmAssessmentSignal is a result
                    // signal in this framework, so we surface the harm-risk
                    // measurement and let a downstream neuron promote it.
                    new MeasurementSignal(m.signalTag(), v, quality, ts);
            case EFFICIENCY -> new EfficiencySignal(m.signalTag(), v, 1.0);
            case MEASUREMENT -> new MeasurementSignal(m.signalTag(), v, quality, ts);
        };
    }

    private static IInputSignal defaultMeasurement(String label, double v,
                                                   Quality quality, long ts) {
        return new MeasurementSignal(label, v, quality, ts);
    }

    private static Quality qualityFor(String safetyStatus) {
        if (safetyStatus == null) return Quality.UNCERTAIN;
        return switch (safetyStatus) {
            case NengoInputFrame.STATUS_OK -> Quality.GOOD;
            case NengoInputFrame.STATUS_DEGRADED -> Quality.UNCERTAIN;
            case NengoInputFrame.STATUS_STOP -> Quality.BAD;
            default -> Quality.UNCERTAIN;
        };
    }

}
