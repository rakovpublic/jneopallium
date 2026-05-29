# Demo 01 — Reactor jacket-temperature cascade control (OPC UA, AUTONOMOUS)

> Bridge: **OPC UA** ([architecture](../opcua-bridge-architecture.md)) ·
> Module: **[industrial](../modules/industrial.md)** ·
> Safety ceiling: **AUTONOMOUS (per-loop)** ·
> External system: an OPC UA server (the public Eclipse Milo demo server for a
> read-only smoke test, or a small `asyncua` Python process emulating the plant
> for the full closed loop).

This is the framework's flagship closed-loop control demo. It shows neuron-derived
setpoints driving a real actuator through the full safety ladder, an interlock
overriding everything when it trips, and the network damping its own oscillation
without a human reconfiguring it.

## Scenario

A continuous stirred-tank reactor (CSTR) holds an exothermic reaction. Reactor
temperature is regulated by a **cascade**: the outer loop (reactor temperature,
`TIC-101`) computes a setpoint for the inner loop (coolant flow, `FIC-101`),
which manipulates a cooling-jacket valve. A safety-rated high-temperature
**interlock** drives the valve fully open and forces the unit to a safe state.
The reaction can go into limit-cycle oscillation under aggressive tuning; the
network must detect and damp it.

## What it demonstrates

| Feature | Where |
|---|---|
| Fast/slow loops — inner flow loop on 1/1, outer temp setpoint on 1/2 | `ProcessingFrequency`, `CycleNeuron` |
| Cascade routing with break-and-hold | `CascadeNeuron` / `ICascadeNeuron` |
| Velocity-form PID with anti-windup, runtime gain scaling | `PIDNeuron` / `IPIDNeuron` |
| Per-loop SHADOW → ADVISORY → AUTONOMOUS promotion | `SafetyGateNeuron` |
| Interlock with direct, un-vetoable authority → fail-safe write | `InterlockNeuron`, `AbstractBridgeOutputAggregator` §2.2.2 |
| Operator override wins for regulatory control | `ActuatorNeuron`, `OverrideRegistry` |
| Automatic, reversible oscillation damping (ACF-at-lag-1 band) | `OscillationMonitorNeuron` / `IOscillationMonitorNeuron` |
| Quality propagation from OPC UA `StatusCode` | `OpcUaSignalMapper` |
| Append-only transparency log of every write | `OpcUaTransparencyLogOutput` |

## Architecture / data flow

```
 OPC UA server (PLC / asyncua sim)
   TIC-101.PV (i=...)  FIC-101.PV   HI_TEMP_ILK         FIC-101.OUT (valve)
        │                 │              │                    ▲
        ▼ subscription    ▼              ▼ subscription        │ writeValues()
  ┌──────────────────────────────────────────────┐            │
  │ MiloOpcUaClientService (latest-value cache,    │           │
  │ alarm queue, sourceTimestamp)                  │           │
  └───┬───────────────┬──────────────┬────────────┘           │
      ▼               ▼              ▼                          │
 OpcUaMeasurement  OpcUaMeasurement  OpcUaAlarmInput            │
   (TIC-101)         (FIC-101)        (HI_TEMP_ILK)             │
      │               │              │                          │
      ▼               ▼              ▼                          │
  ┌──────────────────────────────────────────────────────────┐│
  │ Industrial pipeline (worker/.../impl/industrial):         ││
  │  Sensor → MeasurementValidator                            ││
  │   ├─ outer: PID(TIC-101) ──► SetpointSignal(FIC-101.SP)   ││
  │   │            via CascadeNeuron                          ││
  │   └─ inner: PID(FIC-101) ──► ActuatorCommandSignal        ││
  │  OscillationMonitor (ACF) ──► scales PID gains / breaks   ││
  │  Interlock(HI_TEMP) ──► InterlockSignal(tripped)          ││
  │  SafetyGate(per-tag mode) ──► ActuatorNeuron              ││
  └───────────────────────────┬──────────────────────────────┘│
                              │ List<IResult>                  │
                              ▼                                 │
  ┌──────────────────────────────────────────────────────────┐│
  │ OpcUaCommandOutputAggregator (AbstractBridgeOutputAggreg.)││
  │  1 interlock tripped → fail-safe write (valve = 100%) ────┼┘
  │  2 override active?  → OVERRIDE_HOLD
  │  3 SHADOW→reject · ADVISORY→need execute · AUTONOMOUS→go
  │  4 clamp → rate-limit → diff-suppress → write → audit
  └───────────────────────────┬──────────────────────────────┘
                              ▼
                    OpcUaTransparencyLogOutput  (JSONL + optional OPC UA node)
```

## Components used

* **Signals** (`worker.net.signals.impl.industrial`): `MeasurementSignal` (1/1),
  `SetpointSignal` (1/2), `ActuatorCommandSignal` (1/1), `InterlockSignal` (1/1),
  `AlarmSignal` (1/1), `OperatorOverrideSignal` (1/1).
* **Neurons** (`worker.net.neuron.impl.industrial`): `SensorNeuron`,
  `MeasurementValidatorNeuron`, `PIDNeuron` ×2 (outer/inner), `CascadeNeuron`,
  `OscillationMonitorNeuron`, `InterlockNeuron`, `SafetyGateNeuron`,
  `ActuatorNeuron`. Optional: `MPCPlanningNeuron` + `ProcessModelNeuron` to
  replace the outer PID with a model-predictive outer loop.
* **Processors** (`worker.signalprocessor.impl.industrial`):
  `MeasurementValidationProcessor`, `MeasurementPIDProcessor`,
  `SetpointPIDProcessor`, `MeasurementOscillationProcessor`,
  `MeasurementInterlockProcessor`, `ActuatorSafetyGateProcessor`,
  `ActuatorDispatchProcessor`, `InterlockModeProcessor`,
  `OperatorOverrideProcessor`.
* **Bridge I/O**: `OpcUaMeasurementInput`, `OpcUaAlarmInput`,
  `OpcUaCommandOutputAggregator`, `OpcUaTransparencyLogOutput`,
  `MiloOpcUaClientService`, `OpcUaBridgeConfigLoader`.

## Configuration

`/tmp/demo01-reactor.yaml`:

```yaml
connection:
  endpointUrl: "opc.tcp://localhost:4840/jneopallium/reactor"
  applicationName: "Jneopallium-Reactor-Demo"
  applicationUri: "urn:rakovpublic:jneopallium:demo01"
  requestTimeout: "PT5S"
  sessionTimeout: "PT2M"
  keepAliveFailuresAllowed: 3
security:
  policy: NONE
  mode: NONE
  auth: { type: "Anonymous" }

reads:
  - loopId: "TIC-101"
    nodeId: "ns=2;s=Reactor.TIC101.PV"
    signalTag: "PLANT.TIC101.PV"
    direction: READ
  - loopId: "FIC-101"
    nodeId: "ns=2;s=Reactor.FIC101.PV"
    signalTag: "PLANT.FIC101.PV"
    direction: READ

writes:
  - loopId: "FIC-101"
    nodeId: "ns=2;s=Reactor.FIC101.OUT"
    signalTag: "PLANT.FIC101.OUT"
    minClampValue: 0.0
    maxClampValue: 100.0
    rampRateMaxPerSec: 25.0           # %/s — protects the valve actuator
    failSafeValue: 100.0              # jacket valve fully open on interlock trip

alarms:
  - loopId: "HI_TEMP_ILK"
    nodeId: "ns=2;s=Reactor.HiTempInterlock"
    signalTag: "PLANT.TIC101.HI_ILK"

perLoopSafetyMode:
  FIC-101: SHADOW                     # promote per the run procedure below

audit:
  localAuditFile: "/tmp/jneopallium-demo01-audit.jsonl"
  writeRejectedToAudit: true
tickInterval: "PT0.1S"
```

## Run procedure

1. **Stand up a plant.** For the full closed loop, run a tiny first-order-plus-
   dead-time jacket simulator that exposes the three nodes above. Any OPC UA
   server works; the quickest is a `python-asyncua` script that integrates
   `dT/dt = (Q_rxn - k·valve·(T - T_cool))/C` and publishes `TIC101.PV`,
   `FIC101.PV`, `FIC101.OUT`, `HiTempInterlock`. For a **read-only smoke test
   only**, point `endpointUrl` at `opc.tcp://milo.digitalpetri.com:62541/milo`
   and keep `writes: []` (this is exactly [`opcua-bridge-demo.md`](../opcua-bridge-demo.md)).

2. **Build and wire the bridge** (jshell or a `main`):

   ```java
   var cfg   = OpcUaBridgeConfigLoader.load(Path.of("/tmp/demo01-reactor.yaml"));
   var svc   = new MiloOpcUaClientService(cfg);
   var audit = new OpcUaTransparencyLogOutput(cfg);

   var ticIn = new OpcUaMeasurementInput("opc-tic", svc, List.of("TIC-101"));
   var ficIn = new OpcUaMeasurementInput("opc-fic", svc, List.of("FIC-101"));
   var ilkIn = new OpcUaAlarmInput("opc-ilk", svc);
   var out   = new OpcUaCommandOutputAggregator(svc, audit, cfg);
   // build the industrial sub-net (validator → PID×2 → cascade →
   // oscillation → interlock → safety-gate → actuator) per modules/industrial.md
   ```

3. **Run in SHADOW.** Start the scheduler. Confirm `TIC-101`/`FIC-101`
   `MeasurementSignal`s arrive each tick and that the aggregator emits
   `verdict:"REJECTED" reason:"SHADOW_MODE"` audit lines — the controller is
   computing valve moves but **not** writing them. This is the dry-run a real
   commissioning would require.

4. **Promote to AUTONOMOUS.** Set `perLoopSafetyMode.FIC-101: AUTONOMOUS` and
   restart (config is intentionally not hot-reloaded — Management of Change).
   The inner loop now writes the clamped, rate-limited valve command; reactor
   temperature should settle to the outer setpoint.

5. **Trip the interlock.** Force `HiTempInterlock = true` in the plant. Within
   one tick the aggregator writes `failSafeValue` (`100.0`) to `FIC101.OUT`
   regardless of the PID output and emits `verdict:"INTERLOCK_TRIP"`. No neuron
   output can override this (§2.2.2).

6. **Demonstrate operator override.** Inject
   `OperatorOverrideSignal(tag="PLANT.FIC101.OUT", kind=MANUAL, value=40.0)`.
   The tag freezes at 40 % for the override TTL; audit lines read
   `verdict:"OVERRIDE_HOLD"`. Override applies to regulatory control, never to
   the interlock.

7. **Demonstrate oscillation damping.** De-tune the inner PID (raise gain) until
   the valve limit-cycles. `OscillationMonitorNeuron` measures ACF-at-lag-1 over
   the configured window and maps severity to an `OscillationIntervention` band
   (`SCALE_WEIGHTS → INJECT_INHIBITION → BREAK_CONNECTION → QUARANTINE_NEURON`).
   Gain scaling and cascade break are both reversed automatically on the next
   scheduler tick once the ACF falls back under threshold.

## Acceptance

* In SHADOW, every proposed write is audited as `REJECTED reason=SHADOW_MODE`
  and the valve node never changes.
* In AUTONOMOUS, reactor `TIC-101.PV` tracks its setpoint within band; valve
  moves are clamped to `[0,100]` and never step faster than `rampRateMaxPerSec`.
* Forcing the interlock writes `100.0` within one tick with verdict
  `INTERLOCK_TRIP`, beating any PID command in the same tick.
* An active operator override holds the tag and produces `OVERRIDE_HOLD`.
* An induced limit cycle is detected and damped, and the intervention is
  released automatically once the oscillation subsides (no permanent
  reconfiguration).
* `/tmp/jneopallium-demo01-audit.jsonl` contains one line per decision with the
  `00-FRAMEWORK §4` shape (`ts, run, verdict, loopId, tag, proposed, effective,
  reason, safetyMode`).

## Safety / regulatory posture

The OPC UA bridge is the one adapter rated **AUTONOMOUS per-loop**, because the
server enforces write authority and the protocol gives synchronous, quality-
stamped reads. Even so, the structural guarantees hold: the interlock is sealed
at construction (`InterlockNeuron.seal()` — later `addInterlock` throws),
operator override authority cannot be disabled
(`IndustrialConfig.setOverrideAlwaysHonoured(false)` throws), and no write
reaches the actuator except through `AbstractBridgeOutputAggregator`. Aligns
with IEC 61508 / IEC 62443 / ISA-18.2 as described in
[`../modules/industrial.md`](../modules/industrial.md). This demo does **not**
replace a certified safety PLC — the interlock here models the supervisory path,
not the SIS.
