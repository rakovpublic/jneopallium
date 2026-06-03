# Demo 07 Report: OpenTelemetry Export-Only Anomaly Summarizer

## Executive Summary

This demo shows export-only observability analysis over OpenTelemetry-like metrics, logs, and traces. It runs through the full local Jneopallium path and emits JSONL/OTel-like anomaly summaries only.

The default full-run verification completed with status `PASS`, processed `90` ticks, and wrote `90` output rows.

## Application Story

The synthetic service, `service-checkout`, has a latency and error-rate spike between ticks 6 and 12. The network exports anomaly summaries with root-cause candidate context and anomaly-window attributes.

## Full-Run Execution Path

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-07-observability-otel-export/
```

The local worker path is:

```text
Entry local -> Runner -> LocalApplication -> JsonlResultAggregator
```

## Network Structure

Layer configuration:

- Layer 0, size 4: metric/log/trace input.
- Layer 1, size 3: anomaly feature extraction.
- Layer 2, size 2: result conversion and root-cause candidate.

Typed signal classes:

- `MetricSignal`
- `TraceSignal`
- `LogEventSignal`
- `ObservabilityAnomalySignal`

Neuron and processor classes:

- `ObservabilityOtelNeuron`
- `ObservabilityOtelResultNeuron`
- `ObservabilityOtelProcessor`
- `ObservabilityOtelDemoInput`

## I/O Logic

Input is deterministic telemetry: latency p95, error rate, saturation, and failed span state. Output is export-only JSONL records with anomaly window start/end attributes.

## Deterministic Behavior

Expected behavior:

- Baseline ticks emit `NORMAL_TELEMETRY_EXPORT`.
- Spike ticks emit `ANOMALY_SUMMARY`.
- Output includes `anomalyWindowStart` and `anomalyWindowEnd`.
- No writeback or control result type is emitted.

## Safety Ceiling

Mode is `EXPORT-ONLY`. The demo does not autoscale, restart services, or write controls.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `90`
- Output rows: `90`
- Assertions passed: local mode, JSONL output exists, aggregator called, export-only mode, no writeback/control, anomaly window reported.

## Real-Bridge Extension

A real OpenTelemetry bridge can replace the deterministic input. Keep this demo export-only and route records to monitoring dashboards, incident queues, or offline evaluation.
