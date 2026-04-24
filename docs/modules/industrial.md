# Industrial Process Control & Digital Twins

> Status: implementation of [`use-case-industrial-process-control.md`](../../use-case-industrial-process-control.md) for [jneopallium](https://github.com/rakovpublic/jneopallium).
> License: BSD 3-Clause.

---

## Abstract

The industrial module unifies regulatory control, supervisory
optimisation, planning, maintenance, and safety in a single
signal-flow network. Every layer is structurally separate from the
safety layer — the interlock neuron is frozen at construction time,
operator override always wins for regulatory control, and every
per-loop deployment mode (SHADOW / ADVISORY / AUTONOMOUS) is enforced
at a dedicated safety gate. The headline operational contribution is
automated, reversible oscillation management: the
`OscillationMonitorNeuron` maps each tagged loop's autocorrelation to
a graduated `OscillationIntervention` band (`SCALE_WEIGHTS /
INJECT_INHIBITION / BREAK_CONNECTION / QUARANTINE_NEURON`) so the
network can unilaterally damp a misbehaving cascade without permanent
reconfiguration.

## Design Principles

1. **Typed timescales.** Measurements and actuator commands run on
   loop 1 / epoch 1; setpoints on 1/2; alarms and interlocks on 1/1;
   supervisory efficiency and batch-state on 2; degradation on 2/3;
   maintenance windows on 2/10. Regulatory latency stays bounded as
   slower layers evolve.
2. **Processors parameterised by interfaces.** Every
   `ISignalProcessor` under
   `worker/signalprocessor/impl/industrial` targets an `I<Neuron>`
   interface from `worker/net/neuron/impl/industrial`. A deployment
   can swap PID, MPC, or sensor implementations without touching any
   processor.
3. **Safety is asymmetric.** An interlock can drive a process to
   safe state but never start one; `InterlockNeuron.seal()` freezes
   the rule set, after which additions throw `IllegalStateException`.
   The SRS is compiled once.
4. **Operator override always wins for regulatory control.**
   `ActuatorNeuron` honours the most-recent `OperatorOverrideSignal`;
   MANUAL freezes the tag at the manual value, BYPASS drops
   subsequent commands. `IndustrialConfig.setOverrideAlwaysHonoured(false)`
   throws.
5. **Per-loop deployment mode.** `SafetyGateNeuron` maps SHADOW →
   `execute=false`, ADVISORY → pass through, AUTONOMOUS → force
   `execute=true`. Plants can run 90% autonomous, 9% advisory, 1%
   shadow simultaneously.
6. **Oscillation management is automatic and reversible.** ACF-at-
   lag-1 drives the intervention band; both PID gain scaling and
   cascade break are reversible at the next scheduler tick.

## Signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial`.

| Signal | Loop/Epoch | Notes |
|---|---|---|
| `MeasurementSignal` | 1/1 | ISA-95 tag, value, `Quality`, wall-clock ms |
| `SetpointSignal` | 1/2 | tag, setpoint, ramp rate, source |
| `ActuatorCommandSignal` | 1/1 | tag, target / current, `execute` |
| `AlarmSignal` | 1/1 | `AlarmPriority`, tag, condition, ts |
| `InterlockSignal` | 1/1 | id, `tripped`, causes |
| `DegradationSignal` | 2/3 | asset id, RUL hours, confidence |
| `EfficiencySignal` | 2/1 | unit id, efficiency, baseline |
| `BatchStateSignal` | 2/2 | batch id, `BatchPhase`, key metrics |
| `OperatorOverrideSignal` | 1/1 | tag, `OverrideKind`, operator, reason, manual value |
| `MaintenanceWindowSignal` | 2/10 | asset id, scheduled tick, duration |

## Neurons

Every concrete neuron implements a matching `I<Neuron>` interface so
processors never depend on the concrete type.

### Layer 0 — field I/O
- `SensorNeuron` / `ISensorNeuron` — reads field values.
- `MeasurementValidatorNeuron` / `IMeasurementValidatorNeuron` —
  range + rate-of-change; downgrades quality but never drops.

### Layer 1 — regulatory
- `PIDNeuron` / `IPIDNeuron` — velocity-form with anti-windup,
  runtime-scalable gains.
- `CascadeNeuron` / `ICascadeNeuron` — routes outer output as inner
  setpoint; `setBroken(true)` freezes on last-good setpoint.
- `FeedForwardNeuron` / `IFeedForwardNeuron` — proportional disturbance
  compensation.

### Layer 2 — supervisory
- `SetpointOptimiserNeuron` / `ISetpointOptimiserNeuron` — efficiency-
  driven setpoint nudges under constraints.
- `AlarmAggregationNeuron` / `IAlarmAggregationNeuron` — ISA-18.2
  suppression + per-minute rate.
- `ModeControllerNeuron` / `IModeControllerNeuron` — STARTUP /
  NORMAL / SHUTDOWN / EMERGENCY; interlock trip always forces
  EMERGENCY, only `resetFromEmergency()` exits.

### Layer 3 — model and memory
- `ProcessModelNeuron` / `IProcessModelNeuron` — first-order plus
  dead time.
- `DegradationModelNeuron` / `IDegradationModelNeuron` — per-asset RUL.
- `ProductQualityModelNeuron` / `IProductQualityModelNeuron` — target
  / tolerance compliance.

### Layer 4 — planning
- `MPCPlanningNeuron` / `IMPCPlanningNeuron` — discrete candidate-move
  search against a `IProcessModelNeuron` forecast.
- `CampaignPlanningNeuron` / `ICampaignPlanningNeuron` — FIFO batch-
  state queue.
- `MaintenanceSchedulingNeuron` / `IMaintenanceSchedulingNeuron` —
  proposes a window leading the predicted end-of-life.

### Layer 5 — action
- `SafetyGateNeuron` / `ISafetyGateNeuron` — per-tag SHADOW /
  ADVISORY / AUTONOMOUS mode.
- `ActuatorNeuron` / `IActuatorNeuron` — dispatcher with operator-
  override authority.
- `InterlockNeuron` / `IInterlockNeuron` — SRS rules sealed at
  construction; `addInterlock` after `seal()` throws.

### Layer 7 — homeostasis
- `OscillationMonitorNeuron` / `IOscillationMonitorNeuron` — ACF-at-
  lag-1 severity, mapped to `OscillationIntervention` bands.
- `EnergyAccountingNeuron` / `IEnergyAccountingNeuron` — production-
  per-kWh efficiency with baseline anchoring.

## Processors

Package: `com.rakovpublic.jneuropallium.worker.signalprocessor.impl.industrial`.

| Signal | Processor(s) |
|---|---|
| `MeasurementSignal` | `MeasurementValidationProcessor`, `MeasurementPIDProcessor`, `MeasurementOscillationProcessor`, `MeasurementInterlockProcessor` |
| `SetpointSignal` | `SetpointPIDProcessor` |
| `ActuatorCommandSignal` | `ActuatorSafetyGateProcessor`, `ActuatorDispatchProcessor` |
| `AlarmSignal` | `AlarmAggregationProcessor` |
| `InterlockSignal` | `InterlockModeProcessor` |
| `DegradationSignal` | `DegradationSchedulingProcessor` |
| `EfficiencySignal` | `EfficiencyOptimiserProcessor` |
| `BatchStateSignal` | `BatchModeProcessor` |
| `OperatorOverrideSignal` | `OperatorOverrideProcessor` |
| `MaintenanceWindowSignal` | `MaintenanceWindowSchedulingProcessor` |

Every processor's `getNeuronClass()` returns an interface —
`IndustrialModuleTest::processors_allInterfaceTyped` asserts this.

## Configuration

`IndustrialConfig` mirrors spec §9 with strongly-typed setters.
`setSafetyMode(null)` throws; `setOverrideAlwaysHonoured(false)`
throws — override authority cannot be disabled at runtime.

```yaml
industrial:
  enabled: true
  tick-rate-hz: 100
  isa-95-level: 2
  safety:
    mode: advisory
    interlocks-source: "srs.yaml"
    interlock-test-interval-ticks: 864000
  oscillation:
    detection-enabled: true
    acf-window-ticks: 200
    circuit-breaker-max-duration-ticks: 6000
  mpc:
    horizon-ticks: 60
    control-horizon-ticks: 10
  maintenance:
    rul-refresh-ticks: 100000
    scheduling-horizon-days: 90
  audit:
    log-every-decision: true
    hash-chain-enabled: true
    retention-days: 2555
  operator-override:
    always-honoured: true            # setter throws on false
    lockout-during-emergency-only: true
```

## Tests

`IndustrialModuleTest` covers 33 cases including:
enum cardinalities, every signal's `ProcessingFrequency`, measurement
validator range + rate-of-change flagging, PID steering + gain
scaling, cascade break-and-hold, feed-forward bias, setpoint
optimiser constraint clamping, alarm suppression, mode-machine
EMERGENCY lockout, process-model response dynamics, degradation RUL
reduction, product-quality compliance, MPC move selection, campaign
FIFO, maintenance window scheduling, safety-gate shadow /
autonomous, operator override (MANUAL + BYPASS), sealed-interlock
immutability, interlock trip + fail-safe command, oscillation
severity detection & intervention band, energy-accounting efficiency,
config validation, and one smoke test per processor plus
`processors_allInterfaceTyped` invariant.

Full worker suite: 445/445 pass (412 prior + 33 new), no regressions.

## Regulatory posture

Aligns with IEC 61508 (functional safety, SIL 2 / SIL 3 subsystems
via physical / network segregation from certified SIS), IEC 62443
(OT cybersecurity — reuse the security module on the boundary),
ISA-95 level 2, ISA-18.2 alarm management, 21 CFR Part 11 (hash-
chained audit trail via the existing `TransparencyLogSignal`).

## Out of scope

Per spec §13:
- Replacement of certified safety PLCs.
- Cloud-round-trip control.
- Affect, curiosity, or in-loop LLM.
- Uncontrolled model updates.

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network
  Modeling: The Jneopallium Approach.* IJSR 13(7).
- IEC 61508:2010 — Functional safety of E/E/PE safety-related systems.
- IEC 62443 — Industrial communication networks — IT security.
- ISA-95 / IEC 62264 — Enterprise-control integration.
- ISA-18.2-2016 — Alarm systems management.
- Seborg, D.E., Edgar, T.F., Mellichamp, D.A., Doyle, F.J. (2016).
  *Process Dynamics and Control.* Wiley.
- Ziegler, J.G., Nichols, N.B. (1942). Optimum Settings for
  Automatic Controllers. *Trans. ASME.*
- Grieves, M., Vickers, J. (2017). Digital Twin. *Transdisciplinary
  Perspectives on Complex Systems.*
