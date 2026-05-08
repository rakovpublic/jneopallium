# Bridge 09 — OpenTelemetry (observability and trust)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** very high — this is the *trust bridge*, not a control bridge. **Safety ceiling:** **EXPORT-ONLY**. There is no read path from OTel back into Jneopallium signals. Data flows out, never in.

## 1. Domain context

[OpenTelemetry](https://opentelemetry.io/) is the CNCF vendor-neutral framework for generating, collecting, and exporting **traces, metrics, and logs**. It is the standard observability substrate for cloud-native software, with established backends (Grafana, Prometheus, Tempo, Jaeger, Datadog, Honeycomb, AWS X-Ray, Splunk).

Most other bridges in this directory expose Jneopallium to the world. The OTel bridge does the opposite: it exposes Jneopallium *to operators, auditors, and security teams*. The only way to trust an autonomous system is to be able to inspect what it is doing in real time — what neurons fired, which harm vetoes intervened, where the safety gate held back a setpoint, which tokens of evidence drove which decision.

The repo's signal types map naturally to OpenTelemetry primitives:

| Jneopallium internal | OTel primitive |
|---|---|
| `TransparencyLogSignal` | log record |
| `HarmVetoSignal` | log record + counter metric (`jneo.harm_veto.count`) |
| `LoopAlertSignal` | log record + gauge |
| `LoopInterventionSignal` | log record + counter |
| `EnergySignal` | gauge metric |
| Tick processing | span (`jneo.tick`) with child spans for each layer |
| Aggregator decisions | span attributes on the relevant aggregator span |

## 2. Maven dependencies

```xml
<!-- OpenTelemetry SDK + autoconfiguration + OTLP exporters -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk</artifactId>
    <version>1.43.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
    <version>1.43.0</version>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-sdk-extension-autoconfigure</artifactId>
    <version>1.43.0</version>
</dependency>
<!-- Bridge log4j logs into OTel -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-log4j-appender-2.17</artifactId>
    <version>2.9.0-alpha</version>
</dependency>
```

## 3. Why export-only

Two reasons:

1. **Direction of trust.** OTel is *how Jneopallium proves what it did*. If the same bridge could *receive* OTel signals and feed them back as inputs, the audit trail's integrity guarantee is undermined — a malicious or buggy upstream signal source could fabricate "evidence" that influences decisions.
2. **Observability data is summary.** A Prometheus counter does not carry the typed semantics the cognitive pipeline needs. If an upstream system has data Jneopallium should reason over, it should reach Jneopallium through the appropriate domain bridge (Kafka, FHIR, OPC UA), not as anonymous OTel.

The bridge's read API is the empty set. The class `OtelInput` does not exist. The only public surface is the export pipeline.

## 4. Architecture

The bridge is **passive** with respect to the network pipeline — it does not run as a `IInitInput` or `IOutputAggregator`. Instead it installs itself as a *listener* on existing signal flows, and as a *Tracer* and *Meter* exposed to the rest of the worker.

```
Jneopallium tick:
  Layer 0 → Layer 1 → ... → Layer 7
        │       │              │
        └───────┴──────────────┴──── OtelInstrumentation.observe(signal, layer)
                                              │
                                              ▼
                                  ┌───────────────────────────┐
                                  │ OpenTelemetry SDK          │
                                  │  • Tracer                  │
                                  │  • Meter                   │
                                  │  • LogProcessor            │
                                  └───────┬──────────┬─────────┘
                                          │          │
                                          ▼          ▼
                                       OTLP/gRPC  Prometheus
                                          │          │
                                       collector   /metrics
                                          │
                              ┌───────────┴───────────┐
                              ▼                        ▼
                         Tempo/Jaeger             Loki/Splunk/SIEM
                         (traces)                 (logs)
```

## 5. Instrumentation surface

A small, focused API the rest of the worker calls:

```java
package com.rakovpublic.jneuropallium.worker.bridge.otel;

public interface OtelInstrumentation {
    /** Wrap one tick. Returns an AutoCloseable Span. */
    Scope tickSpan(long run);

    /** Record one signal observation under the current scope. */
    void observe(ISignal signal, String layerName);

    /** Record one harm veto. */
    void harmVeto(String reason, String evidenceNeurons);

    /** Record an aggregator verdict. */
    void aggregatorVerdict(String bridge, String tag, String verdict, String reason,
                           Double proposed, Double effective);

    /** Record one BridgeAuditRecord. */
    void audit(BridgeAuditRecord record);
}
```

A no-op implementation (`NoopOtelInstrumentation`) is the default; the active SDK-backed implementation is wired only when the OTel bridge is enabled. This guarantees the worker has zero observability cost in deployments that don't want OTel.

## 6. Configuration

```yaml
otel:
  serviceName: "jneopallium-bridge"
  serviceVersion: "1.0-SNAPSHOT"
  serviceInstanceId: "${HOSTNAME}"

  exporter:
    type: "OTLP_GRPC"                # or "OTLP_HTTP", "PROMETHEUS_PUSH", "NONE"
    endpoint: "http://otel-collector.observability.svc:4317"
    timeoutMs: 10000
    headers:
      authorization: "Bearer ${OTEL_TOKEN}"

  resourceAttributes:
    deployment.environment: "production"
    plant.id: "PLANT-01"

  metrics:
    enabled: true
    intervalMs: 10000

  traces:
    enabled: true
    samplerRatio: 0.1                # 10% of ticks; harm-veto ticks ALWAYS sampled

  logs:
    enabled: true

  redaction:
    redactSignalTags: false           # set true for HIPAA-restricted deployments
    redactPatterns: []                # additional regex patterns to scrub from log bodies
```

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/otel/
├── OtelBridgeConfig.java
├── OtelBridgeConfigLoader.java
├── OtelInstrumentation.java          (interface)
├── NoopOtelInstrumentation.java
├── SdkOtelInstrumentation.java       (active SDK-backed implementation)
├── OtelLayerTracer.java              (instruments per-layer spans)
├── OtelMeterRegistry.java            (named counters/gauges for harm vetoes,
│                                       loop interventions, audits)
└── package-info.java
```

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Trace + log path. Per-tick span with layer children. Bridge audit records → log records with structured attributes. |
| 2 | Metrics path. Counters: `jneo.harm_veto.count`, `jneo.loop_intervention.count`, `jneo.audit.applied.count` etc. Gauges: `jneo.energy`, `jneo.loop.amplitude`. |
| 3 | Sampler refinement: always sample any tick that produced a harm veto, an interlock trip, or an aggregator REJECTED verdict. Helps post-incident analysis. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | Spans emitted | Run a 1-second simulated tick stream against a local OTel collector | Tempo shows one parent `jneo.tick` span with child layer spans |
| **S8** | Counters increment | Force one harm veto | `jneo.harm_veto.count` increments by 1 in the configured collector |
| **S9** | Always-sample for vetoes | Sampler ratio 0.1, but veto fires on a tick | That tick's span is exported regardless |
| **S10** | Exporter offline | OTel collector unreachable | Worker continues; OTel SDK buffers + drops oldest; no impact on critical path |
| **S11** | No read API | Inspect public types in `bridge.otel` | No `OtelInput` class exists; aggregator class also doesn't exist |
| **S12** | Redaction | Tag matches a redaction regex | Tag appears as `***` in exported attributes; counts and structure preserved |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Performance overhead on the critical path | Default sampler 10%; veto/intervention always sampled. Counters are lock-free. Synthetic benchmark in CI must show < 5 % overhead vs noop. |
| R2 | Sensitive data leaking into traces | Redaction config + per-deployment audit. Industrial deployments default to redacting raw measurement values; clinical deployments redact signal tags. |
| R3 | OTel SDK transitive deps breaking the worker classpath | Use the BOM (`opentelemetry-bom`) consistently; verify no shading conflicts with gRPC. |
| R4 | Backpressure when the collector is slow | OTel SDK's `BatchSpanProcessor` drops on overflow; document operator alerting on `otel.exporter.dropped` counter. |

## 11. References

* OpenTelemetry — `https://opentelemetry.io/`.
* OTel Java SDK — `https://github.com/open-telemetry/opentelemetry-java`.
* OTel BOM and instrumentation — `https://opentelemetry.io/docs/instrumentation/java/`.
