# Demo 02 Report: Predictive Pump Fleet Maintenance

## Executive Summary

This demo shows a multi-asset predictive maintenance application for an MQTT/Sparkplug-like pump fleet. It runs through the full Jneopallium local path with generated model packaging, generated layer metadata, generated context JSON, deterministic pump telemetry, and JSONL result aggregation.

The default full-run verification completed with status `PASS`, processed `1000` ticks, and wrote `1000` output rows.

## Application Story

The synthetic fleet contains ten pumps. Pump `pump-03` has a rising vibration and bearing-temperature trend. Pump `pump-07` goes offline after tick 8. Other pumps remain healthy enough that they do not receive maintenance advisories in the same run window.

## Full-Run Execution Path

The demo is launched through:

```text
Entry -> Runner -> LocalApplication
```

with `mode=local`, a generated `demo-model.jar`, `DemoJsonContext`, and generated `context.json`.

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-02-pump-fleet-maintenance/
```

## Network Structure

Layer configuration:

- Layer 0, size 5: pump telemetry input.
- Layer 1, size 4: feature extraction.
- Layer 2, size 3: health estimation.
- Layer 3, size 2: advisory planning and priority scoring.
- Layer 4, size 3: result conversion.

Typed signal classes:

- `VibrationSignal`
- `BearingTempSignal`
- `DeviceEventSignal`
- `DegradationSignal`
- `RemainingUsefulLifeSignal`
- `MaintenanceAdvisorySignal`

Neuron and processor classes:

- `PumpFleetNeuron`
- `PumpFleetResultNeuron`
- `PumpFleetProcessor`
- `PumpFleetDemoInput`

## I/O Logic

Input is a deterministic mock stream for ten pumps. It emits vibration and bearing-temperature signals for every pump and device-event signals for offline or periodic state changes.

Output is JSONL advisory data. The aggregator records remaining useful life values, advisory decisions, offline events, and audit reasons.

## Deterministic Behavior

Expected behavior:

- `pump-03` receives `MAINTENANCE_ADVISORY` after its remaining useful life drops.
- `pump-07` receives `DEVICE_OFFLINE_ADVISORY`.
- `pump-00` remains healthy and receives zero maintenance advisories in the same window.

## Safety Ceiling

Mode is `ADVISORY`. The demo does not autonomously write to a pump controller or publish maintenance commands. It produces monitoring and planning recommendations only.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `1000`
- Output rows: `1000`
- `pump03MinRul`: `40.0`
- `pump00MinRul`: `950.0`
- Assertions passed: local mode, JSONL output exists, aggregator called, degrading pump lower RUL than healthy pump, maintenance advisory emitted, offline pump advisory emitted, healthy pump zero maintenance advisory.

## Real-Bridge Extension

A real MQTT/Sparkplug bridge can replace `PumpFleetDemoInput`. Keep the advisory-only output mode, asset allowlists, and explicit offline-state audit records.
