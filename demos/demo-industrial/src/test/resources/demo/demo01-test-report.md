# Demo-01 Test Report — Reactor Jacket-Temperature Cascade Control

**Date:** 2026-05-29  
**Branch:** `claude/compassionate-bohr-yI2cL`  
**Spec:** [`demo-01-reactor-cascade-control.md`](../../../../../../../../demo-01-reactor-cascade-control.md)

---

## Summary

| Suite | Tests | Passed | Failed | Skipped |
|-------|------:|-------:|-------:|--------:|
| `Demo01ReactorCascadeControlTest` | 6 | 6 | 0 | 0 |
| `Demo01ConfigYamlTest` | 1 | 1 | 0 | 0 |
| **Total (new demo-01 tests)** | **7** | **7** | **0** | **0** |

All 7 tests pass as part of the full worker suite (839 tests, 0 failures).

---

## How to Run

```bash
# run only the demo-01 tests
mvn -pl worker test -Dtest=Demo01ReactorCascadeControlTest,Demo01ConfigYamlTest

# full worker suite
mvn -pl worker test

# narrated walk-through (writes audit JSONL to /tmp/jneopallium-demo01-audit.jsonl)
mvn -q -pl worker compile dependency:build-classpath -Dmdep.outputFile=/tmp/cp.txt
java -cp "worker/target/classes:$(cat /tmp/cp.txt)" \
  com.rakovpublic.jneuropallium.worker.demo.industrial.Demo01ReactorCascadeControl
```

---

## Test-by-Test Results

### T-1 `shadow_rejectsEveryWrite_andValveNeverChanges`

**Acceptance bullet:** *In SHADOW, every proposed write is audited as `REJECTED reason=SHADOW_MODE` and the valve node never changes.*

| Step | Observation | Result |
|------|-------------|--------|
| 80 ticks in `SafetyMode.SHADOW` | `valveWriteCount == 0` | PASS |
| Valve position after 80 ticks | unchanged from initial 50 % | PASS |
| All valve audit lines | `verdict="REJECTED" reason="SHADOW_MODE"` | PASS |
| Controller still proposes moves | at least one audit line present | PASS |

---

### T-2 `autonomous_tracksSetpoint_writesClampedAndRateLimited`

**Acceptance bullet:** *In AUTONOMOUS, reactor `TIC-101.PV` tracks its setpoint within band; valve moves are clamped to `[0,100]` and never step faster than `rampRateMaxPerSec`.*

| Step | Observation | Result |
|------|-------------|--------|
| 1 500 ticks in `SafetyMode.AUTONOMOUS` | T within ±5 °C of 80 °C setpoint | PASS |
| Valve writes reach plant | `valveWriteCount > 0` | PASS |
| All `APPLIED` effective values | `0.0 ≤ eff ≤ 100.0` | PASS |
| Consecutive `APPLIED` steps | `|Δeff| ≤ 2.5 %/tick` (25 %/s × 0.1 s) | PASS |

---

### T-3 `interlock_writesFailSafeWithinOneTick_beatingPidCommand`

**Acceptance bullet:** *Forcing the interlock writes `100.0` within one tick with verdict `INTERLOCK_TRIP`, beating any PID command in the same tick.*

| Step | Observation | Result |
|------|-------------|--------|
| 300 settle ticks | PID actively commanding | PASS |
| Pin reactor temperature to 115 °C (> 110 °C threshold) | — | — |
| After exactly 1 tick | `plant.getValve() == 100.0` | PASS |
| Audit for that tick contains | `verdict="INTERLOCK_TRIP" effective=100.0` | PASS |
| PID command in same tick | `verdict="REJECTED" reason="INTERLOCK_HOLD"` | PASS |

---

### T-4 `operatorOverride_holdsTag_andEmitsOverrideHold`

**Acceptance bullet:** *An active operator override holds the tag and produces `OVERRIDE_HOLD`.*

| Step | Observation | Result |
|------|-------------|--------|
| 300 settle ticks | valve actively written | PASS |
| Inject `OperatorOverrideSignal(VALVE_TAG, MANUAL, "op-7", 40.0)` | — | — |
| 20 subsequent ticks | `valveWriteCount` unchanged since override | PASS |
| Audit | ≥ 20 lines with `verdict="OVERRIDE_HOLD" tag=PLANT.FIC101.OUT` | PASS |

---

### T-5 `oscillation_detectedDampedThenReleased_noPermanentReconfig`

**Acceptance bullet:** *An induced limit cycle is detected and damped, and the intervention is released automatically once the oscillation subsides (no permanent reconfiguration).*

| Step | Observation | Result |
|------|-------------|--------|
| 300 baseline ticks | `currentIntervention() == NONE` | PASS |
| `setOperatorGainScale(8.0)` — de-tune inner KP ×8 | — | — |
| 120 ticks at elevated gain | `maxSeveritySeen() > 0.30` (ACF-at-lag-1) | PASS |
| 120 ticks at elevated gain | `interventionFired() == true` | PASS |
| `setOperatorGainScale(1.0)` — restore nominal | — | — |
| 150 release ticks | `currentIntervention() == NONE` | PASS |
| After release | `gainsRestored() == true` (no permanent reconfiguration) | PASS |
| After release | `oscillationSeverity() < 0.30` | PASS |

**Intervention bands (ACF-at-lag-1 severity ρ):**

| Band | Severity range | Action |
|------|----------------|--------|
| `NONE` | < 0.30 | nominal operation |
| `SCALE_WEIGHTS` | 0.30–0.60 | inner KP × 0.5 |
| `INJECT_INHIBITION` | 0.60–0.85 | inner KP × 0.25 |
| `BREAK_CONNECTION` | 0.85–0.98 | KP × 0.2, cascade broken |
| `QUARANTINE_NEURON` | ≥ 0.98 | KP × 0.1, cascade broken |

All damping is **automatically reversed** when severity falls below the band threshold. No config change or restart is required.

---

### T-6 `audit_everyLineHasFrameworkShape`

**Acceptance bullet:** *`/tmp/jneopallium-demo01-audit.jsonl` contains one line per decision with the `00-FRAMEWORK §4` shape.*

| Step | Observation | Result |
|------|-------------|--------|
| 200 ticks in AUTONOMOUS | audit file non-empty | PASS |
| Every JSONL line | contains all 9 required keys | PASS |
| Required keys | `ts, run, verdict, loopId, tag, proposed, effective, reason, safetyMode` | PASS |
| `ts` and `verdict` | non-null on every line | PASS |

---

### T-7 `Demo01ConfigYamlTest — documentedYaml_loadsAndMatchesInCodeConfig`

**Coverage:** Proves `demo01-reactor.yaml` parses under the loader's strict `FAIL_ON_UNKNOWN_PROPERTIES` contract and agrees exactly with `Demo01Config.build()`.

| Assertion | Expected | Result |
|-----------|----------|--------|
| Endpoint URL | `opc.tcp://localhost:4840/jneopallium/reactor` | PASS |
| Tick interval | `PT0.1S` (100 ms) | PASS |
| Per-loop safety mode for `FIC-101` | `SHADOW` (initial; promoted by restart) | PASS |
| Read bindings count | 2 (`TIC-101.PV`, `FIC-101.PV`) | PASS |
| Write bindings count | 1 (`FIC-101.OUT`) | PASS |
| Write: `signalTag` | `PLANT.FIC101.OUT` | PASS |
| Write: `failSafeValue` | 100.0 % | PASS |
| Write: `rampRateMaxPerSec` | 25.0 %/s | PASS |
| Write: clamp | `[0.0, 100.0]` | PASS |
| YAML vs in-code cross-check | endpoint, tick, write tag, failSafe, alarm tag match | PASS |

---

## Audit JSONL — Sample Lines

Every line written by `OpcUaTransparencyLogOutput` conforms to `00-FRAMEWORK §4`.

**SHADOW — write proposed but rejected:**
```json
{"ts":1740000001000,"run":1,"verdict":"REJECTED","loopId":"FIC-101","tag":"PLANT.FIC101.OUT","proposed":52.3,"effective":null,"reason":"SHADOW_MODE","safetyMode":"SHADOW"}
```

**AUTONOMOUS — write applied:**
```json
{"ts":1740000010000,"run":2,"verdict":"APPLIED","loopId":"FIC-101","tag":"PLANT.FIC101.OUT","proposed":55.1,"effective":55.1,"reason":null,"safetyMode":"AUTONOMOUS"}
```

**Interlock trip — fail-safe write beats PID:**
```json
{"ts":1740000040000,"run":3,"verdict":"INTERLOCK_TRIP","loopId":"FIC-101","tag":"PLANT.FIC101.OUT","proposed":100.0,"effective":100.0,"reason":"INTERLOCK_TRIP","safetyMode":"AUTONOMOUS"}
```

**PID command held by interlock in same tick:**
```json
{"ts":1740000040000,"run":3,"verdict":"REJECTED","loopId":"FIC-101","tag":"PLANT.FIC101.OUT","proposed":57.4,"effective":null,"reason":"INTERLOCK_HOLD","safetyMode":"AUTONOMOUS"}
```

**Operator override holding the tag:**
```json
{"ts":1740000050000,"run":4,"verdict":"OVERRIDE_HOLD","loopId":"FIC-101","tag":"PLANT.FIC101.OUT","proposed":61.2,"effective":null,"reason":"OVERRIDE_HOLD","safetyMode":"AUTONOMOUS"}
```

---

## Architecture Under Test

```
OpcUaMeasurementInput(TIC-101)
OpcUaMeasurementInput(FIC-101)   ─┐
OpcUaAlarmInput(HI_TEMP_ILK)     │
                                  ▼
             MeasurementValidatorNeuron  (range check, quality)
                                  │
             PIDNeuron (outer, reverse-acting)
             outer Kp=-2.0  Ki=-0.4  Kd=0.0
                                  │ SetpointSignal → inner SP
             CascadeNeuron        │ (break-and-hold on oscillation)
                                  │
             PIDNeuron (inner)
             inner Kp=0.5   Ki=2.5   Kd=0.0
                                  │ ActuatorCommandSignal
             OscillationMonitorNeuron  (ACF-at-lag-1)
                                  │
             InterlockNeuron      │ (sealed SRS; direct authority)
                                  │
             SafetyGateNeuron     │ (SHADOW / ADVISORY / AUTONOMOUS)
                                  │
             ActuatorNeuron       │ (operator override wins)
                                  ▼
             OpcUaCommandOutputAggregator
               ├── ramp + clamp enforcement
               ├── interlock write (bypass PID)
               └── OpcUaTransparencyLogOutput  →  audit.jsonl
```

**Transport:** `SimulatedReactorOpcUaService` (in-process, no network) for tests and the runner. Swap to a real `MiloOpcUaClientService` + `asyncua` plant server for over-the-wire runs; no other code changes.

---

## OPC UA Node Bindings

| Node ID | Signal tag | Direction | Loop ID |
|---------|-----------|-----------|---------|
| `ns=2;s=Reactor.TIC101.PV` | `PLANT.TIC101.PV` | READ | `TIC-101` |
| `ns=2;s=Reactor.FIC101.PV` | `PLANT.FIC101.PV` | READ | `FIC-101` |
| `ns=2;s=Reactor.FIC101.OUT` | `PLANT.FIC101.OUT` | WRITE | `FIC-101` |
| `ns=2;s=Reactor.HiTempInterlock` | `PLANT.TIC101.HI_ILK` | READ (alarm) | `HI_TEMP_ILK` |

---

## Plant Parameters (FOPDT CSTR model)

| Parameter | Value | Description |
|-----------|-------|-------------|
| `T₀` | 80 °C | initial reactor temperature |
| `F₀` | 50 % | initial coolant flow |
| `T_cool` | 20 °C | coolant inlet temperature |
| `C` (thermal mass) | 200 (J/°C) | heat capacity |
| `K` (cool gain) | 6 W/%flow/°C | cooling coefficient |
| `τ_flow` | 0.12 s | first-order flow lag |
| Interlock threshold | 110 °C | supervisory HI_TEMP trip level |
| Fail-safe valve | 100 % open | maximum coolant on trip |
| `dt` | 0.1 s | integration step |
