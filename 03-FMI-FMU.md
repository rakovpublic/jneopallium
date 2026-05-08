# Bridge 03 — FMI / FMU (simulation and digital twins)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** extremely high — this is the testbench for every other bridge. **Safety ceiling:** `AUTONOMOUS` (sim-only — the bridge's outputs go into a simulation, never to physical equipment).

## 1. Domain context

[FMI](https://fmi-standard.org/) (Functional Mock-up Interface) is a free standard for exchanging dynamic simulation models. An FMU (Functional Mock-up Unit) is a ZIP container with an XML model description, native C binaries for the supported platforms, and optionally model source. FMI is supported by 280+ tools (Modelica, Simulink, Dymola, OpenModelica, Amesim, GT-SUITE, JModelica, FMPy, …).

For Jneopallium this is the **most important bridge**: it lets us prove the whole pipeline — measurements → safety gate → harm gate → planning → setpoints — against a physically-realistic simulator before any real-hardware integration. Every other bridge in this directory should ship with at least one FMU-driven integration test.

The bridge supports both **FMI 2.0** and **FMI 3.0**. Both use the same Java import API.

## 2. Maven dependency

```xml
<!-- Reference Java importer for FMI 2.0 + 3.0 — actively maintained,
     Apache 2.0 licensed -->
<dependency>
    <groupId>org.javafmi</groupId>
    <artifactId>javafmi-2.18</artifactId>
    <version>2.18.0</version>
</dependency>
```

If `javafmi` is unavailable in your registry, the alternative is JNA bindings to the native FMI libraries (more code, more brittle); spec assumes the importer.

## 3. Why this bridge is worth doing first

* It is the **only** bridge that can run on a developer laptop end-to-end without external infrastructure.
* It exercises the same `IInitInput`/`IOutputAggregator` boundary as every "real" bridge — proving the boundary is correct.
* It enables CI: every other bridge spec's S1–S6 scenarios are simulator-driven, and this is the simulator.
* It enables non-trivial demos: a tank-temperature FMU + Jneopallium PID + safety gate + interlock + operator override, all auditable, all reversible, all in one process.

## 4. Architecture

The FMI bridge is unusual among bridges because the "external system" is **in the same JVM** (or at least the same machine). There is no network, no reconnect, no quality-from-status-code.

```
┌──────────────────────┐    fmu.read(...)         ┌────────────────────────┐
│ FMU (.fmu file)      │ ────────────────────────▶│ FmuClientService       │
│  • Modelica-compiled │                          │  • single-step driver  │
│  • Simulink export   │ ◀──── fmu.write(...) ─── │  • simulated clock     │
│  • OpenModelica      │                          │  • variable cache      │
└──────────────────────┘                          └─────┬───────────┬──────┘
                                                        │           │
                                                ┌───────▼─┐  ┌──────▼──────┐
                                                │ FmuMea  │  │ FmuEvent    │
                                                │ surement│  │ Input       │
                                                │ Input   │  │             │
                                                └─────────┘  └─────────────┘
                                                              ▼
                                  [Standard pipeline → FmuCommandOutputAggregator]
                                                              ▼
                                                  fmu.setReal(reference, value)
                                                  fmu.doStep(t, h)
```

### 4.1 Clock

Two modes, selected in YAML:

* **Real-time clock:** `tickInterval` is the wall-clock period; `doStep(t, h)` is called with `h = tickInterval`. Used for HIL-style demos where Jneopallium runs at its production rate.
* **As-fast-as-possible clock:** `doStep` is called in a tight loop; one Jneopallium tick == one FMU step. Used for scenario sweeps and CI.

The choice is critical: the same harness will report wildly different stability properties under the two clocks. CI must use as-fast-as-possible; integration demos use real-time.

## 5. Signal mapping

| FMU variable | Jneopallium signal | Notes |
|---|---|---|
| Output `Real` | `MeasurementSignal` | `Quality.GOOD` always (sim is deterministic). To test quality propagation, configure the FMU itself to emit a "quality" output and bind it to a separate signal. |
| Output `Boolean` (alarm flag) | `AlarmSignal` | Severity in YAML. |
| Output `Integer` (state) | `BatchStateSignal` or domain enum | Per binding. |
| Input `Real` | `SetpointSignal` / `ActuatorCommandSignal` | Through aggregator → safety chain. |
| Input `Boolean` (interlock command) | `ActuatorCommandSignal` with discrete domain | Interlock fail-safe writes go here on trip. |

No new signal types; reuse `industrial/`.

## 6. Configuration

```yaml
fmu:
  path: "./testdata/tank_temperature.fmu"
  loggingOn: false                # FMI debug logging
  toleranceDefined: true
  tolerance: 1e-6

clock:
  mode: "REAL_TIME"               # or "AS_FAST_AS_POSSIBLE"
  startTime: 0.0
  stepSize: 0.25                  # seconds per simulated tick

reads:
  - bindingId: "TANK-TEMP"
    fmuVariable: "tank.T"
    signalTag: "PLANT.TANK01.TEMP"

writes:
  - bindingId: "HEATER-Q"
    fmuVariable: "heater.Q"
    signalTag: "PLANT.HEATER01.SP"
    failSafeValue: 0.0
    minClampValue: 0.0
    maxClampValue: 50000.0       # Watts

events:
  - bindingId: "OVERTEMP"
    fmuVariable: "alarm.over_temperature"
    signalTag: "PLANT.TANK01.OVERTEMP"
    severity: "CRITICAL"

audit:
  localAuditFile: "./out/fmu-audit.jsonl"
  writeRejectedToAudit: true

perTagSafetyMode:
  TANK-TEMP: AUTONOMOUS    # safe — the "field" is a simulator
```

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/fmi/
├── FmiBridgeConfig.java
├── FmiBridgeConfigLoader.java
├── FmuVariableBinding.java
├── FmuSignalMapper.java
├── FmuClientService.java          (instantiate, init, doStep loop)
├── FmuMeasurementInput.java
├── FmuEventInput.java
├── FmuCommandOutputAggregator.java
└── package-info.java

worker/src/test/resources/fmu/
├── tank_temperature.fmu           (committed; generated from a 30-line Modelica file)
├── cstr.fmu                       (continuously stirred tank reactor demo)
└── README.md                      (how to regenerate from .mo source)
```

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | Single FMU instance, read-only. Verify variable resolution, type coercion, real-time vs as-fast clock. |
| 2 | Write path through the aggregator. Single PID neuron + tank FMU closed loop in CI. |
| 3 | Multi-FMU compositions (one PV plant + one battery + one inverter, glued by Jneopallium). Out of scope for v1; documented as future work. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | Closed-loop in CI | tank.fmu + PIDNeuron, AUTONOMOUS, target 60 °C | Within 30 simulated seconds, `tank.T` settles within ±0.5 °C of setpoint |
| **S8** | Interlock fires | tank.fmu with overtemperature trigger; safety gate observes | When `over_temperature` becomes true, aggregator writes `failSafeValue=0` to `heater.Q` regardless of PID output, audit `verdict=INTERLOCK_TRIP` |
| **S9** | Operator override | Force `OperatorOverrideSignal(tag=PLANT.HEATER01.SP, manualValue=10000)` for 5 s | During the 5 s window, no PID-derived value reaches the FMU; `heater.Q` is held at 10000 |
| **S10** | Mode switching mid-run | Start `SHADOW`, switch to `AUTONOMOUS` after 5 s | Until 5 s: aggregator records but doesn't write; after: closed-loop control kicks in |
| **S11** | FMU exception | FMU raises `fmiError` mid-step | Bridge logs at ERROR, emits an audit `verdict=FAILED`, halts the simulation cleanly (no zombie FMU instance) |
| **S12** | Determinism | Same FMU + same seed + AS_FAST_AS_POSSIBLE, run twice | Audit logs are byte-identical |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Native binary platform mismatch (FMU compiled for win64, host is linux64) | Bridge documents the platform requirement at config-load time; bundled test FMUs are cross-compiled for linux64 + osx-arm64 + win64. |
| R2 | FMU resource leak on dirty shutdown | `FmuClientService.close()` always calls `fmu.terminate()` then `fmu.freeInstance()` even if `doStep` failed. |
| R3 | Real-time mode jitter on busy CI | Default to `AS_FAST_AS_POSSIBLE` for CI; only HIL demos use real-time. |
| R4 | Modelica vs Simulink-exported FMU semantics drift | Test FMUs are Modelica-sourced; document the `.mo` file in `src/test/resources/fmu/README.md` so anyone can regenerate. |

## 11. References

* FMI standard — `https://fmi-standard.org/`.
* javafmi — `https://github.com/CATIA-Systems/JavaFMI` or its current home; check before merge.
* OpenModelica — free Modelica compiler suitable for generating the test FMUs.
