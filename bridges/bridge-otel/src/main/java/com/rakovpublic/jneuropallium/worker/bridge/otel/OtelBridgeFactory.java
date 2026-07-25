/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.otel;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.metrics.OtlpHttpMetricExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

import java.time.Duration;
import java.util.Map;

/**
 * Wires the OpenTelemetry SDK from an {@link OtelBridgeConfig}
 * (09-OPENTELEMETRY.md §6).
 *
 * <p>Behaviour:
 * <ul>
 *   <li>{@link OtelBridgeConfig.ExporterConfig.ExporterType#NONE} returns the
 *       {@link NoopOtelInstrumentation}, so callers can use the same
 *       {@link OtelInstrumentation} reference unconditionally.</li>
 *   <li>Otherwise the SDK is built with a {@link Sampler#parentBasedTraceIdRatio
 *       parent-based ratio sampler} per §6 {@code samplerRatio}, and OTLP
 *       gRPC/HTTP exporters per §6 {@code exporter.type}.</li>
 *   <li>Resource attributes pull in {@code serviceName}, {@code serviceVersion},
 *       {@code serviceInstanceId} plus everything under
 *       {@code resourceAttributes:}.</li>
 * </ul>
 */
public final class OtelBridgeFactory {

    private OtelBridgeFactory() {}

    public static OtelInstrumentation create(OtelBridgeConfig config) {
        if (config == null
                || config.exporter().type() == OtelBridgeConfig.ExporterConfig.ExporterType.NONE
                || (!config.traces().enabled()
                    && !config.metrics().enabled()
                    && !config.logs().enabled())) {
            return NoopOtelInstrumentation.INSTANCE;
        }
        OpenTelemetrySdk sdk = buildSdk(config);
        return new SdkOtelInstrumentation(sdk, sdk, config);
    }

    /**
     * Build an OTel SDK from the bridge config. Visible for testing so unit
     * tests can plug in {@code InMemorySpanExporter} via
     * {@link #buildSdk(OtelBridgeConfig, SpanExporter, MetricExporter, LogRecordExporter)}.
     */
    public static OpenTelemetrySdk buildSdk(OtelBridgeConfig config) {
        return buildSdk(config,
                spanExporter(config),
                metricExporter(config),
                logExporter(config));
    }

    public static OpenTelemetrySdk buildSdk(OtelBridgeConfig config,
                                            SpanExporter spanExporter,
                                            MetricExporter metricExporter,
                                            LogRecordExporter logExporter) {
        Resource resource = buildResource(config);

        SdkTracerProvider tracerProvider = null;
        if (config.traces().enabled() && spanExporter != null) {
            tracerProvider = SdkTracerProvider.builder()
                    .setResource(resource)
                    .setSampler(Sampler.parentBased(
                            Sampler.traceIdRatioBased(config.traces().samplerRatio())))
                    .addSpanProcessor(BatchSpanProcessor.builder(spanExporter).build())
                    .build();
        }

        SdkMeterProvider meterProvider = null;
        if (config.metrics().enabled() && metricExporter != null) {
            meterProvider = SdkMeterProvider.builder()
                    .setResource(resource)
                    .registerMetricReader(PeriodicMetricReader.builder(metricExporter)
                            .setInterval(Duration.ofMillis(config.metrics().intervalMs()))
                            .build())
                    .build();
        }

        SdkLoggerProvider loggerProvider = null;
        if (config.logs().enabled() && logExporter != null) {
            loggerProvider = SdkLoggerProvider.builder()
                    .setResource(resource)
                    .addLogRecordProcessor(BatchLogRecordProcessor.builder(logExporter).build())
                    .build();
        }

        var b = OpenTelemetrySdk.builder();
        if (tracerProvider != null) b.setTracerProvider(tracerProvider);
        if (meterProvider != null)  b.setMeterProvider(meterProvider);
        if (loggerProvider != null) b.setLoggerProvider(loggerProvider);
        return b.build();
    }

    /**
     * Convenience for tests that want to drive a {@link SdkOtelInstrumentation}
     * directly (no exporters needed). Falls through to the same builder above.
     */
    public static OtelInstrumentation forTesting(OtelBridgeConfig config,
                                                 OpenTelemetrySdk sdk) {
        return new SdkOtelInstrumentation(sdk, sdk, config);
    }

    private static Resource buildResource(OtelBridgeConfig config) {
        AttributesBuilder b = Attributes.builder()
                .put(AttributeKey.stringKey("service.name"), config.serviceName())
                .put(AttributeKey.stringKey("service.version"), config.serviceVersion())
                .put(AttributeKey.stringKey("service.instance.id"), config.serviceInstanceId());
        for (Map.Entry<String, String> e : config.resourceAttributes().entrySet()) {
            b.put(AttributeKey.stringKey(e.getKey()), e.getValue());
        }
        return Resource.getDefault().merge(Resource.create(b.build()));
    }

    private static SpanExporter spanExporter(OtelBridgeConfig config) {
        if (!config.traces().enabled()) return null;
        return switch (config.exporter().type()) {
            case OTLP_GRPC -> {
                var b = OtlpGrpcSpanExporter.builder()
                        .setTimeout(Duration.ofMillis(config.exporter().timeoutMs()));
                if (config.exporter().endpoint() != null) b.setEndpoint(config.exporter().endpoint());
                config.exporter().headers().forEach(b::addHeader);
                yield b.build();
            }
            case OTLP_HTTP -> {
                var b = OtlpHttpSpanExporter.builder()
                        .setTimeout(Duration.ofMillis(config.exporter().timeoutMs()));
                if (config.exporter().endpoint() != null) b.setEndpoint(config.exporter().endpoint());
                config.exporter().headers().forEach(b::addHeader);
                yield b.build();
            }
            // Prometheus push isn't a span exporter; spans go nowhere in this
            // mode. Returning null disables the tracer provider entirely.
            case PROMETHEUS_PUSH, NONE -> null;
        };
    }

    private static MetricExporter metricExporter(OtelBridgeConfig config) {
        if (!config.metrics().enabled()) return null;
        return switch (config.exporter().type()) {
            case OTLP_GRPC -> {
                var b = OtlpGrpcMetricExporter.builder()
                        .setTimeout(Duration.ofMillis(config.exporter().timeoutMs()));
                if (config.exporter().endpoint() != null) b.setEndpoint(config.exporter().endpoint());
                config.exporter().headers().forEach(b::addHeader);
                yield b.build();
            }
            case OTLP_HTTP, PROMETHEUS_PUSH -> {
                var b = OtlpHttpMetricExporter.builder()
                        .setTimeout(Duration.ofMillis(config.exporter().timeoutMs()));
                if (config.exporter().endpoint() != null) b.setEndpoint(config.exporter().endpoint());
                config.exporter().headers().forEach(b::addHeader);
                yield b.build();
            }
            case NONE -> null;
        };
    }

    private static LogRecordExporter logExporter(OtelBridgeConfig config) {
        if (!config.logs().enabled()) return null;
        return switch (config.exporter().type()) {
            case OTLP_GRPC -> {
                var b = OtlpGrpcLogRecordExporter.builder()
                        .setTimeout(Duration.ofMillis(config.exporter().timeoutMs()));
                if (config.exporter().endpoint() != null) b.setEndpoint(config.exporter().endpoint());
                config.exporter().headers().forEach(b::addHeader);
                yield b.build();
            }
            case OTLP_HTTP -> {
                var b = OtlpHttpLogRecordExporter.builder()
                        .setTimeout(Duration.ofMillis(config.exporter().timeoutMs()));
                if (config.exporter().endpoint() != null) b.setEndpoint(config.exporter().endpoint());
                config.exporter().headers().forEach(b::addHeader);
                yield b.build();
            }
            case PROMETHEUS_PUSH, NONE -> null;
        };
    }
}
