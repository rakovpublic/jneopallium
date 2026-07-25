/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleGauge;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * Named counters / gauges / histograms for the OTel bridge
 * (09-OPENTELEMETRY.md §7, §8 phase 2).
 *
 * <p>All instruments are created lazily and cached so the hot path is
 * lock-free (§10 R1).
 */
final class OtelMeterRegistry implements AutoCloseable {

    static final AttributeKey<String> BRIDGE      = AttributeKey.stringKey("jneo.bridge");
    static final AttributeKey<String> VERDICT     = AttributeKey.stringKey("jneo.verdict");
    static final AttributeKey<String> REASON      = AttributeKey.stringKey("jneo.reason");
    static final AttributeKey<String> TAG         = AttributeKey.stringKey("jneo.tag");
    static final AttributeKey<String> LOOP_ID     = AttributeKey.stringKey("jneo.loop_id");
    static final AttributeKey<String> SIGNAL_TYPE = AttributeKey.stringKey("jneo.signal_type");
    static final AttributeKey<String> LAYER       = AttributeKey.stringKey("jneo.layer");

    private final Meter meter;

    final LongCounter harmVetoCount;
    final LongCounter loopInterventionCount;
    final LongCounter auditAppliedCount;
    final LongCounter auditRejectedCount;
    final LongCounter auditFailedCount;
    final LongCounter aggregatorVerdictCount;
    final LongCounter signalObservedCount;
    final DoubleHistogram setpointDelta;

    /** Latest energy observation, exposed via an async gauge. */
    private final DoubleAdder lastEnergy = new DoubleAdder();
    /** Latest loop-amplitude per loopId, exposed via an async multi-key gauge. */
    private final ConcurrentMap<String, Double> loopAmplitudes = new ConcurrentHashMap<>();

    private final ObservableDoubleGauge energyGauge;
    private final ObservableDoubleGauge loopAmplitudeGauge;

    OtelMeterRegistry(Meter meter) {
        this.meter = meter;

        this.harmVetoCount = meter.counterBuilder("jneo.harm_veto.count")
                .setDescription("Count of HarmVetoSignal events emitted by the worker")
                .setUnit("1")
                .build();

        this.loopInterventionCount = meter.counterBuilder("jneo.loop_intervention.count")
                .setDescription("Count of LoopInterventionSignal events emitted")
                .setUnit("1")
                .build();

        this.auditAppliedCount = meter.counterBuilder("jneo.audit.applied.count")
                .setDescription("Count of bridge audit records with verdict=APPLIED")
                .setUnit("1")
                .build();

        this.auditRejectedCount = meter.counterBuilder("jneo.audit.rejected.count")
                .setDescription("Count of bridge audit records with verdict=REJECTED")
                .setUnit("1")
                .build();

        this.auditFailedCount = meter.counterBuilder("jneo.audit.failed.count")
                .setDescription("Count of bridge audit records with verdict=FAILED, INTERLOCK_TRIP or OVERRIDE_HOLD")
                .setUnit("1")
                .build();

        this.aggregatorVerdictCount = meter.counterBuilder("jneo.aggregator.verdict.count")
                .setDescription("Bridge aggregator verdicts, attributed by bridge + verdict")
                .setUnit("1")
                .build();

        this.signalObservedCount = meter.counterBuilder("jneo.signal.observed.count")
                .setDescription("Signal observations recorded by the OTel instrumentation, by signal type")
                .setUnit("1")
                .build();

        this.setpointDelta = meter.histogramBuilder("jneo.aggregator.setpoint.delta")
                .setDescription("Difference between proposed and effective setpoint per aggregator write")
                .setUnit("1")
                .build();

        this.energyGauge = meter.gaugeBuilder("jneo.energy")
                .setDescription("Current EnergySignal value")
                .setUnit("1")
                .buildWithCallback(measurement -> measurement.record(lastEnergy.sum()));

        this.loopAmplitudeGauge = meter.gaugeBuilder("jneo.loop.amplitude")
                .setDescription("Current LoopAlertSignal amplitude, per loopId")
                .setUnit("1")
                .buildWithCallback(measurement -> {
                    for (var e : loopAmplitudes.entrySet()) {
                        measurement.record(e.getValue(), Attributes.of(LOOP_ID, e.getKey()));
                    }
                });
    }

    void setEnergy(double value) {
        lastEnergy.reset();
        lastEnergy.add(value);
    }

    void setLoopAmplitude(String loopId, double amplitude) {
        if (loopId == null) loopId = "unknown";
        loopAmplitudes.put(loopId, amplitude);
    }

    Meter meter() { return meter; }

    @Override
    public void close() {
        try { energyGauge.close(); } catch (RuntimeException ignored) {}
        try { loopAmplitudeGauge.close(); } catch (RuntimeException ignored) {}
        loopAmplitudes.clear();
    }
}
