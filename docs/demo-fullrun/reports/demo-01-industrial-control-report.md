# Demo 01 Report: Industrial Process Control

## Executive Summary

This demo shows a safety-gated continuous-control application for an OPC-UA-like reactor loop. It runs Jneopallium in local mode with a generated model JAR, generated layer metadata, a generated context JSON, deterministic synthetic plant inputs, and a JSONL result aggregator.

The default full-run verification completed with status `PASS`, processed `200` ticks, and wrote `200` output rows.

## Application Story

The synthetic plant has one reactor asset, `reactor-a`. Its input stream includes temperature process value, flow process value, valve feedback, high-temperature alarm state, and manual override state. The network recommends cooling valve behavior under normal conditions, forces a fail-safe action during a high-temperature alarm, and records a held/rejected command when manual override is active.

## Full-Run Execution Path

The demo is launched through the worker entry point:

```text
Entry -> Runner.runNet(mode=local, jarPath=demo-model.jar, contextClass=DemoJsonContext, contextJson=context.json)
```

Generated artifacts are written under:

```text
target/jneopallium-fullrun-demos/demo-01-industrial-control/
```

Important files:

- `demo-model.jar`: generated model JAR containing demo classes.
- `context.json`: generated `DemoJsonContext` properties.
- `layers/`: generated layer metadata consumed by `FileLayersMeta`.
- `results.jsonl`: one JSON line per run.
- `audit.jsonl`: aggregator audit trail.
- `manifest.json`: per-demo status and behavior assertions.

## Network Structure

Layer configuration:

- Layer 0, size 4: sensor input normalization.
- Layer 1, size 3: cascade-control feature processing.
- Layer 2, size 2: interlock rule processing.
- Layer 3, size 2: safety gate processing.
- Layer 4, size 3: result conversion.

Typed signal classes:

- `TemperatureSignal`
- `FlowSignal`
- `AlarmSignal`
- `ControlDemandSignal`
- `ValveCommandSignal`
- `SafetyVetoSignal`

Neuron and processor classes:

- `IndustrialControlNeuron`
- `IndustrialControlResultNeuron`
- `IndustrialControlProcessor`
- `IndustrialDemoInput`

## I/O Logic

Input is supplied by a deterministic local mock. The input source emits sensor and alarm signals on each tick and uses `OneToAllFirstLayerInputStrategy` to seed the first layer.

The output path is `JsonlResultAggregator`, which records result type, neuron id, layer id, signal type, value, confidence, safety mode, decision, reason, and attributes.

## Deterministic Behavior

Expected behavior:

- Normal ticks emit `VALVE_COMMAND`.
- Tick 5 injects a high-temperature alarm and emits `FAIL_SAFE_COMMAND` within one tick.
- Tick 10 injects manual override and emits `HELD_COMMAND`.

## Safety Ceiling

Mode is `AUTONOMOUS-MOCK`. The demo represents autonomous control logic only inside a deterministic local plant mock. It is not connected to real equipment and must not be treated as a production control write path.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `200`
- Output rows: `200`
- Assertions passed: local mode, JSONL output exists, aggregator called, at least 100 result rows, normal valve command, forced alarm fail-safe within one tick, manual override held.

## Real-Bridge Extension

A real OPC UA bridge can replace `IndustrialDemoInput` later. The recommended path is to keep the same typed signals and safety gate layer, add strict tag allowlists, and preserve audit output for every command recommendation.
