/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.data.MetricDataType;
import io.opentelemetry.sdk.metrics.data.PointData;
import io.opentelemetry.sdk.metrics.data.SumData;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 09-OPENTELEMETRY.md §9 — exercises scenarios S7..S12 against the SDK
 * implementation using the OTel in-memory exporters as a fake collector.
 */
class OtelBridgeIntegrationTest {

    private InMemorySpanExporter spans;
    private InMemoryMetricReader metrics;
    private InMemoryLogRecordExporter logs;
    private OpenTelemetrySdk sdk;
    private SdkOtelInstrumentation otel;

    @BeforeEach
    void setUp() {
        // Tests need deterministic span export — use ratio=1.0. The S9 test
        // separately exercises the always-sample-on-veto marker behaviour.
        OtelBridgeConfig config = baseConfig(1.0, false, List.of());
        wireSdk(config);
    }

    @AfterEach
    void tearDown() {
        if (otel != null) otel.close();
        if (sdk != null)  sdk.close();
    }

    private OtelBridgeConfig baseConfig(double samplerRatio,
                                         boolean redactSignalTags,
                                         List<String> redactPatterns) {
        return new OtelBridgeConfig(
                "jneopallium-bridge",
                "test",
                "host-1",
                new OtelBridgeConfig.ExporterConfig(
                        OtelBridgeConfig.ExporterConfig.ExporterType.OTLP_GRPC,
                        "http://localhost:4317",
                        10_000L,
                        Map.of()),
                Map.of("deployment.environment", "test"),
                new OtelBridgeConfig.MetricsConfig(true, 1_000L),
                new OtelBridgeConfig.TracesConfig(true, samplerRatio),
                new OtelBridgeConfig.LogsConfig(true),
                new OtelBridgeConfig.RedactionConfig(redactSignalTags, redactPatterns));
    }

    /** Build an SDK pointed at the in-memory exporters with the given sampler. */
    private void wireSdk(OtelBridgeConfig config) {
        spans = InMemorySpanExporter.create();
        metrics = InMemoryMetricReader.create();
        logs = InMemoryLogRecordExporter.create();

        SdkTracerProvider tp = SdkTracerProvider.builder()
                .setSampler(Sampler.parentBased(
                        Sampler.traceIdRatioBased(config.traces().samplerRatio())))
                .addSpanProcessor(SimpleSpanProcessor.create(spans))
                .build();
        SdkMeterProvider mp = SdkMeterProvider.builder()
                .registerMetricReader(metrics)
                .build();
        SdkLoggerProvider lp = SdkLoggerProvider.builder()
                .addLogRecordProcessor(SimpleLogRecordProcessor.create(logs))
                .build();

        sdk = OpenTelemetrySdk.builder()
                .setTracerProvider(tp)
                .setMeterProvider(mp)
                .setLoggerProvider(lp)
                .build();

        otel = new SdkOtelInstrumentation(sdk, sdk, config);
    }

    // ---------------------------------------------------------------- S7
    @Test
    void s7_tickSpanWithLayerChildren() {
        try (OtelInstrumentation.Scope tick = otel.tickSpan(42L)) {
            try (OtelInstrumentation.Scope ignored = otel.layerSpan("L0")) {
                otel.observe(new FakeSignal("FakeMeasurement"), "L0");
            }
            try (OtelInstrumentation.Scope ignored = otel.layerSpan("L1")) {
                otel.observe(new FakeSignal("FakeMeasurement"), "L1");
            }
            // Force-sample so the parent tick span is exported even at low ratios.
            otel.forceSample("test_s7");
        }

        List<SpanData> exported = spans.getFinishedSpanItems();
        assertFalse(exported.isEmpty(), "expected at least one span exported");

        SpanData parent = exported.stream()
                .filter(s -> "jneo.tick".equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no jneo.tick span"));
        assertEquals(42L, parent.getAttributes().get(SdkOtelInstrumentation.TICK_RUN));

        List<String> childNames = exported.stream()
                .filter(s -> s.getParentSpanId().equals(parent.getSpanId()))
                .map(SpanData::getName)
                .sorted()
                .toList();
        assertEquals(List.of("jneo.layer.L0", "jneo.layer.L1"), childNames);
    }

    // ---------------------------------------------------------------- S8
    @Test
    void s8_harmVetoCounterIncrements() {
        otel.harmVeto("policy_safety", "n1,n2,n3");

        Collection<MetricData> data = metrics.collectAllMetrics();
        long count = sumByName(data, "jneo.harm_veto.count");
        assertEquals(1L, count, "jneo.harm_veto.count must be exactly 1");

        // Veto must also produce a structured log record (§5).
        assertTrue(logs.getFinishedLogRecordItems().stream()
                .anyMatch(r -> "jneo.harm_veto".equals(r.getBodyValue().getValue())));
    }

    // ---------------------------------------------------------------- S9
    @Test
    void s9_alwaysSampleOnVeto() {
        // §9 S9: a tick that produced a veto must be exported regardless of
        // sampler ratio. The §8 phase-3 sampler refinement is implemented as
        // a marker attribute (jneo.force_sample=true, jneo.force_sample.cause)
        // that downstream tail samplers / collectors recognise. This test
        // asserts the marker is set on the tick span and is preserved end-to
        // -end through the exporter.
        try (OtelInstrumentation.Scope ignored = otel.tickSpan(7L)) {
            otel.harmVeto("policy_safety", "n1");
        }

        SpanData tick = spans.getFinishedSpanItems().stream()
                .filter(s -> "jneo.tick".equals(s.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no tick span emitted"));
        assertEquals(Boolean.TRUE,
                tick.getAttributes().get(SdkOtelInstrumentation.FORCE_FLAG));
        assertEquals("harm_veto",
                tick.getAttributes().get(SdkOtelInstrumentation.FORCE_CAUSE));
    }

    @Test
    void s9_forceSampleIsSafeWithoutActiveSpan() {
        // The instrumentation must not crash when forceSample is called with
        // no recording span on the current thread (ratio=0.0 path).
        if (otel != null) otel.close();
        if (sdk != null) sdk.close();
        wireSdk(baseConfig(0.0, false, List.of()));
        // No tickSpan(): forceSample must be a no-op rather than throw.
        otel.forceSample("orphan_call");
        otel.harmVeto("orphan_veto", null);
    }

    // ---------------------------------------------------------------- S10
    @Test
    void s10_exporterOfflineDoesNotImpactWorker() {
        // Build an SdkOtelInstrumentation with a real OTLP gRPC exporter
        // pointed at an unreachable port. Calls must complete promptly and
        // not throw.
        OtelBridgeConfig cfg = baseConfig(1.0, false, List.of());
        OpenTelemetrySdk realSdk = OtelBridgeFactory.buildSdk(
                new OtelBridgeConfig(
                        cfg.serviceName(), cfg.serviceVersion(), cfg.serviceInstanceId(),
                        new OtelBridgeConfig.ExporterConfig(
                                OtelBridgeConfig.ExporterConfig.ExporterType.OTLP_GRPC,
                                "http://127.0.0.1:1",   // closed port
                                500L,
                                Map.of()),
                        cfg.resourceAttributes(),
                        cfg.metrics(), cfg.traces(), cfg.logs(), cfg.redaction()));
        SdkOtelInstrumentation realOtel = new SdkOtelInstrumentation(realSdk, realSdk, cfg);
        try {
            long started = System.nanoTime();
            for (int i = 0; i < 50; i++) {
                try (var s = realOtel.tickSpan(i)) {
                    realOtel.harmVeto("offline_test", "n0");
                    realOtel.audit(new BridgeAuditRecord(
                            System.currentTimeMillis(), i, "fmi",
                            BridgeAuditRecord.Verdict.APPLIED, "L1", "tag1",
                            1.0, 1.0, "ok", BridgeSafetyMode.AUTONOMOUS, List.of()));
                }
            }
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            assertTrue(elapsedMs < 5_000,
                    "calls must not block on dead exporter; took " + elapsedMs + " ms");
        } finally {
            realOtel.close();
        }
    }

    // ---------------------------------------------------------------- S11
    @Test
    void s11_noReadApi() throws Exception {
        // Walk every public class in com.rakovpublic.jneuropallium.worker.bridge.otel
        // and assert that none of them is a worker input or aggregator.
        String pkg = "com.rakovpublic.jneuropallium.worker.bridge.otel";
        List<Class<?>> classes = classesIn(pkg);
        assertFalse(classes.isEmpty(),
                "package scan returned zero classes (test infra problem)");

        Class<?> initInput = tryLoad("com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput");
        Class<?> outputAggregator = tryLoad("com.rakovpublic.jneuropallium.worker.application.IOutputAggregator");
        assertNotNull(initInput, "expected to find IInitInput");
        assertNotNull(outputAggregator, "expected to find IOutputAggregator");

        for (Class<?> c : classes) {
            if (Modifier.isAbstract(c.getModifiers())) continue;
            assertFalse(initInput.isAssignableFrom(c),
                    "OTel bridge MUST be export-only: " + c.getName() + " implements IInitInput");
            assertFalse(outputAggregator.isAssignableFrom(c),
                    "OTel bridge MUST be export-only: " + c.getName() + " implements IOutputAggregator");
            assertFalse(c.getSimpleName().equals("OtelInput"),
                    "Class OtelInput must not exist (§3, §11 S11)");
            assertFalse(c.getSimpleName().endsWith("CommandOutputAggregator"),
                    "OTel bridge must not ship a command aggregator: " + c.getName());
        }
    }

    // ---------------------------------------------------------------- S12
    @Test
    void s12_redaction() {
        // Re-wire with redactSignalTags=true and a regex on raw values.
        if (otel != null) otel.close();
        if (sdk != null) sdk.close();
        wireSdk(baseConfig(1.0, true, List.of("secret-\\d+")));

        try (OtelInstrumentation.Scope ignored = otel.tickSpan(1L)) {
            otel.aggregatorVerdict("fmi", "PLANT-01.tag1", "REJECTED",
                    "ADVISORY_HOLD", 1.0, 0.5);
            otel.audit(new BridgeAuditRecord(
                    System.currentTimeMillis(), 1L, "fmi",
                    BridgeAuditRecord.Verdict.REJECTED,
                    "L1",
                    "secret-1234",
                    1.0, null, "ADVISORY_HOLD",
                    BridgeSafetyMode.ADVISORY,
                    List.of("n42")));
        }

        // Counters and structure preserved.
        Collection<MetricData> data = metrics.collectAllMetrics();
        assertTrue(sumByName(data, "jneo.aggregator.verdict.count") >= 1);
        assertTrue(sumByName(data, "jneo.audit.rejected.count") >= 1);

        // Tag must appear redacted in the audit log record.
        boolean foundRedacted = logs.getFinishedLogRecordItems().stream()
                .filter(r -> "jneo.bridge.audit".equals(r.getBodyValue().getValue()))
                .anyMatch(r -> "***".equals(
                        r.getAttributes().get(OtelMeterRegistry.TAG)));
        assertTrue(foundRedacted, "tag must be redacted to *** when redactSignalTags=true");
    }

    // ---------------------------------------------------------------- helpers
    private static long sumByName(Collection<MetricData> all, String name) {
        long total = 0;
        for (MetricData md : all) {
            if (!md.getName().equals(name)) continue;
            if (md.getType() != MetricDataType.LONG_SUM) continue;
            SumData<? extends PointData> sd = md.getLongSumData();
            for (PointData p : sd.getPoints()) {
                total += ((io.opentelemetry.sdk.metrics.data.LongPointData) p).getValue();
            }
        }
        return total;
    }

    private static Class<?> tryLoad(String fqcn) {
        try { return Class.forName(fqcn); } catch (ClassNotFoundException e) { return null; }
    }

    /** Walk the class-path entry that contains the given package and list classes. */
    private static List<Class<?>> classesIn(String pkg) throws IOException {
        String packagePath = pkg.replace('.', '/');
        ClassLoader cl = OtelBridgeIntegrationTest.class.getClassLoader();
        URLClassLoader ucl = (cl instanceof URLClassLoader) ? (URLClassLoader) cl : null;
        List<Class<?>> result = new ArrayList<>();

        // Find an arbitrary class in the package on disk, then walk its directory.
        var marker = cl.getResource(packagePath);
        if (marker != null && "file".equals(marker.getProtocol())) {
            Path dir = Path.of(URI.create(marker.toString()));
            try (var s = Files.walk(dir, 1)) {
                s.filter(p -> p.toString().endsWith(".class"))
                        .forEach(p -> {
                            String fqcn = pkg + "." + p.getFileName().toString().replace(".class", "");
                            tryAdd(result, fqcn);
                        });
            }
            return result;
        }

        // Fallback: scan jar entries on the URLClassLoader path.
        if (ucl == null) return result;
        for (java.net.URL url : ucl.getURLs()) {
            if (!url.toString().endsWith(".jar")) continue;
            try (InputStream in = url.openStream(); ZipInputStream zin = new ZipInputStream(in)) {
                ZipEntry e;
                while ((e = zin.getNextEntry()) != null) {
                    String n = e.getName();
                    if (n.startsWith(packagePath) && n.endsWith(".class")
                            && !n.substring(packagePath.length() + 1).contains("/")) {
                        String fqcn = n.replace('/', '.').replace(".class", "");
                        tryAdd(result, fqcn);
                    }
                }
            }
        }
        return result;
    }

    private static void tryAdd(List<Class<?>> dst, String fqcn) {
        try { dst.add(Class.forName(fqcn)); } catch (Throwable ignored) {}
    }

    /**
     * Minimal {@link ISignal} stub for §5 {@code observe} testing.
     */
    private static final class FakeSignal implements ISignal<String> {
        private final String name;
        FakeSignal(String name) { this.name = name; }
        @Override public String getValue() { return name; }
        @Override @SuppressWarnings({"rawtypes","unchecked"})
        public Class<? extends ISignal<String>> getCurrentSignalClass() { return (Class) FakeSignal.class; }
        @Override public Class<String> getParamClass() { return String.class; }
        @Override public String toJSON() { return "{\"name\":\"" + name + "\"}"; }
        @Override public String getDescription() { return name; }
        @Override public boolean canUseProcessorForParent() { return false; }
        @Override public ISignal<String> prepareSignalToNextStep() { return this; }
        @Override public int getSourceLayerId() { return 0; }
        @Override public void setSourceLayerId(int layerId) {}
        @Override public Long getSourceNeuronId() { return 0L; }
        @Override public void setSourceNeuronId(Long neuronId) {}
        @Override public int getTimeAlive() { return 0; }
        @Override public boolean isFromExternalNet() { return false; }
        @Override public void setFromExternalNet(boolean fromExternalNet) {}
        @Override public String getInputName() { return ""; }
        @Override public void setInputName(String inputName) {}
        @Override public void setCurrentInnerLoop(Integer loop) {}
        @Override public Integer getCurrentInnerLoop() { return 0; }
        @Override public void setInnerLoop(Integer loop) {}
        @Override public Integer getInnerLoop() { return 0; }
        @Override public void setEpoch(Long epoch) {}
        @Override public Long getEpoch() { return 0L; }
        @Override public void setLoop(Integer loop) {}
        @Override public Integer getLoop() { return 0; }
        @Override public boolean isNeedToRemoveDuringLearning() { return false; }
        @Override public boolean isNeedToProcessDuringLearning() { return false; }
        @Override public void setNeedToRemoveDuringLearning(boolean value) {}
        @Override public String getName() { return name; }
        @Override public void setName(String n) {}
        @Override @SuppressWarnings("unchecked")
        public <K extends ISignal<String>> K copySignal() { return (K) new FakeSignal(name); }
    }

}
