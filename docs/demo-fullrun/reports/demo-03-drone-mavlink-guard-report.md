# Demo 03 Report: Drone MAVLink Mission Guard

## Executive Summary

This demo shows a robotics mission safety guard for simulated MAVLink-style telemetry. It runs through `Entry`, `Runner`, and `LocalApplication`, using a generated model JAR and generated local-run configuration.

The default full-run verification completed with status `PASS`, processed `50` ticks, and wrote `50` output rows.

## Application Story

The synthetic drone, `drone-sim-1`, emits battery, GPS quality, altitude, geofence distance, and mission command signals. The mission guard allows normal waypoint commands, vetoes a geofence-violating command, and recommends return-to-home when battery is low.

## Full-Run Execution Path

The demo is launched in local mode through the worker entry point and writes generated artifacts under:

```text
target/jneopallium-fullrun-demos/demo-03-drone-mavlink-guard/
```

The generated `entry.log` records the real `Entry` invocation and context class.

## Network Structure

Layer configuration:

- Layer 0, size 5: drone telemetry input.
- Layer 1, size 3: risk feature extraction.
- Layer 2, size 2: mission guard and command veto.
- Layer 3, size 2: result conversion.

Typed signal classes:

- `BatterySignal`
- `GpsSignal`
- `AltitudeSignal`
- `GeofenceSignal`
- `MissionCommandSignal`
- `MissionGuardSignal`

Neuron and processor classes:

- `DroneMissionNeuron`
- `DroneMissionResultNeuron`
- `DroneMissionProcessor`
- `DroneMissionDemoInput`

## I/O Logic

Input is deterministic simulated mission telemetry. The local mock injects a geofence violation at tick 6 and low-battery risk starting at tick 12.

Output is JSONL advisory and veto data, including command decision, confidence, reason, and safety mode.

## Deterministic Behavior

Expected behavior:

- Normal mission commands emit `COMMAND_ALLOWED`.
- A geofence-violating command emits `COMMAND_VETO`.
- Low battery emits `RETURN_TO_HOME_ADVISORY`.

## Safety Ceiling

Mode is `SIM-ONLY`. The demo does not control a real drone. It demonstrates guard logic against deterministic simulated telemetry only.

## Verification Evidence

Default run evidence from `summary.json`:

- Status: `PASS`
- Ticks: `50`
- Output rows: `50`
- `allowedCount`: `22`
- `vetoCount`: `2`
- Assertions passed: local mode, JSONL output exists, aggregator called, command allowed, command vetoed, return-to-home advisory.

## Real-Bridge Extension

A real MAVLink bridge can replace the mock input stream. The first production-safe version should remain advisory or simulation-only until a separate safety case, vehicle allowlist, operator confirmation path, and command arbitration layer are added.
