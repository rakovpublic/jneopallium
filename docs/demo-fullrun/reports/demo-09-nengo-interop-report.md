# Demo 09 Report: Nengo Interoperability

## Executive Summary

This demo shows how Jneopallium can sit between external neural or simulation systems using a local mock Nengo vector stream. It runs the real local worker path and emits vector output with confidence.

The default full-run verification completed with status `PASS`, processed `75` ticks, and wrote `75` output lines containing `150` result rows.

## Application Story

The mock Nengo stream emits a deterministic two-channel vector based on sine and cosine values. Jneopallium transforms the vector, applies temporal/advisory processing stages, and writes vector-classification output with confidence.

## Full-Run Execution Path

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-09-nengo-interop/
```

The run is launched through:

```text
Entry -> Runner -> LocalApplication
```

with `mode=local`, generated model JAR, generated layer metadata, and generated `DemoJsonContext`.

## Network Structure

Layer configuration:

- Layer 0, size 3: Nengo-like vector input.
- Layer 1, size 3: feature extraction and temporal smoothing.
- Layer 2, size 2: decision/advisory processing.
- Layer 3, size 2: result conversion.

Typed signal classes:

- `NengoVectorSignal`
- `TemporalFeatureSignal`
- `NengoOutputSignal`

Neuron and processor classes:

- `NengoInteropNeuron`
- `NengoInteropResultNeuron`
- `NengoInteropProcessor`
- `NengoInteropDemoInput`

## I/O Logic

Input is a deterministic local vector stream. The demo does not require Python or a Nengo installation. Output is JSONL vector output with numeric value, output-vector attribute, decision, and confidence.

## Deterministic Behavior

Expected behavior:

- Each tick emits vector output rows.
- Output includes confidence greater than zero.
- Positive and negative vector classifications are derived from the mock vector value.

## Safety Ceiling

Mode is `ADVISORY`. The demo demonstrates interoperability and classification/advisory output only.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `75`
- Output lines: `75`
- Result rows: `150`
- Assertions passed: local mode, JSONL output exists, aggregator called, expected vector rows, confidence emitted.

## Real-Bridge Extension

A real Nengo process can replace `NengoInteropDemoInput` by streaming CSV, JSON, or IPC vector data into the same typed signal contract. Keep the local mock as the CI-safe default.
