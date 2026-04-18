# Use Case: Industrial Process Control & Digital Twins

> **Framework:** [jneopallium](https://github.com/rakovpublic/jneopallium) + autonomous-AI architecture + embodiment module.
> **Domain:** Chemical plants, power grids, manufacturing lines, water treatment, HVAC optimisation.
> **Why jneopallium fits:** Industrial control is the original multi-timescale, safety-critical, auditable-decision domain. The framework's `LoopCircuitBreakerNeuron` addresses the real operational problem of oscillating cascaded PID loops; the `HarmGateNeuron` + `TransparencyLogSignal` pairing maps directly onto IEC 61508 / ISO 26262 / IEC 62443 safety and auditability requirements.

---

## 1. Problem framing

Industrial processes need:

- **Control layer** — PID-style real-time regulation (milliseconds).
- **Supervisory layer** — setpoint management, optimisation (seconds → minutes).
- **Planning layer** — production scheduling, campaign planning (hours → days).
- **Maintenance layer** — degradation tracking, remaining-useful-life estimation (days → years).
- **Safety layer** — interlocks, emergency shutdowns; must have independent authority over all others.

Conventional DCS/SCADA stacks implement these as separate siloed products with hand-maintained integration. jneopallium unifies them in one signal-flow model while keeping the safety layer architecturally separate (via the existing harm-discriminator module).

The framework's temporary-quarantine-then-recovery pattern is *exactly* what cascaded control loops need to damp oscillation without permanent reconfiguration — a classic unsolved operational problem.

---

## 2. Mapping to core framework

| Control concept | jneopallium primitive |
|---|---|
| Sensor reading | `SensorySignal` (fast / 1) |
| PID controller | Small neuron with three processors (P, I, D) |
| Setpoint | Slot in `WorkingMemoryNeuron` or `GoalUpdateSignal` |
| Actuator command | `MotorCommandSignal` gated by `HarmGateNeuron` |
| Interlock | Hard constraint in `EthicalPriorityNeuron` |
| MPC horizon | `PlanningNeuron` simulation horizon |
| Oscillation damping | `LoopCircuitBreakerNeuron` graduated interventions |
| Drift / degradation | Weight drift in `LongTermMemoryNeuron` + `MetaplasticityNeuron` |
| Maintenance window | `CircadianNeuron` analogue with domain-specific phase |
| Digital twin | The running jneopallium network instance itself |
| Shadow mode | `RecommendationNeuron` analogue — decisions logged, not executed |

---

## 3. Domain-specific signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial`

| Signal | Loop / Epoch | Payload |
|---|---|---|
| `MeasurementSignal` | 1 / 1 | `String tag` (ISA-95 ref), `double value`, `Quality quality` (GOOD, BAD, UNCERTAIN), `long timestamp` |
| `SetpointSignal` | 1 / 2 | `String tag`, `double setpoint`, `double rampRate`, `String source` |
| `ActuatorCommandSignal` | 1 / 1 | `String tag`, `double targetValue`, `double currentValue`, `boolean execute` |
| `AlarmSignal` | 1 / 1 | `AlarmPriority prio`, `String tag`, `String conditionCode`, `long timestamp` |
| `InterlockSignal` | 1 / 1 | `String interlockId`, `boolean tripped`, `List<String> causes` |
| `DegradationSignal` | 2 / 3 | `String assetId`, `double remainingUsefulLifeHours`, `double confidence` |
| `EfficiencySignal` | 2 / 1 | `String unitId`, `double efficiency`, `double baseline` |
| `BatchStateSignal` | 2 / 2 | `String batchId`, `BatchPhase phase`, `Map<String,Double> keyMetrics` |
| `OperatorOverrideSignal` | 1 / 1 | `String tag`, `OverrideKind kind` (MANUAL, BYPASS), `String operatorId`, `String reason` |
| `MaintenanceWindowSignal` | 2 / 10 | `String assetId`, `long scheduledTick`, `int durationTicks` |

### Timestamping

Every signal in this domain carries a wall-clock timestamp in addition to the jneopallium tick. Industrial compliance requires correlation with external systems; tick number alone is insufficient. Add a default timestamp field to the base `AbstractSignal` for this module's signals, do not alter core.

---

## 4. Domain-specific neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial`

### Layer 0 — field I/O

| Class | Loop / Epoch | Role |
|---|---|---|
| `SensorNeuron` | 1 / 1 | Reads from OPC-UA / Modbus / MQTT Sparkplug; quality-aware; emits `MeasurementSignal` |
| `MeasurementValidatorNeuron` | 1 / 1 | Range / rate-of-change / quality filter; flags suspicious readings without discarding them |

### Layer 1 — regulatory control

| Class | Loop / Epoch | Role |
|---|---|---|
| `PIDNeuron` | 1 / 1 | Specialised `Neuron` with three stateful processors for P, I, D terms |
| `CascadeNeuron` | 1 / 1 | Routes output of one PID as setpoint to another; standard cascade pattern |
| `FeedForwardNeuron` | 1 / 1 | Disturbance-feedforward compensation |

### Layer 2 — supervisory

| Class | Loop / Epoch | Role |
|---|---|---|
| `SetpointOptimiserNeuron` | 1 / 3 | Optimises setpoints against `EfficiencySignal` target under constraints |
| `AlarmAggregationNeuron` | 1 / 2 | ISA 18.2 alarm management — suppress standing alarms, group related, compute rates |
| `ModeControllerNeuron` | 2 / 1 | Startup / normal / shutdown / emergency mode state machine; gates downstream neurons |

### Layer 3 — model and memory

| Class | Loop / Epoch | Role |
|---|---|---|
| `ProcessModelNeuron` | 2 / 1 | First-principles or data-driven plant model for `ConsequenceModelNeuron` |
| `DegradationModelNeuron` | 2 / 3 | Per-asset RUL estimator (fatigue, corrosion, catalyst age) |
| `ProductQualityModelNeuron` | 2 / 2 | Predicts product spec compliance from current conditions |

### Layer 4 — planning

| Class | Loop / Epoch | Role |
|---|---|---|
| `MPCPlanningNeuron` | 1 / 3 | Model-predictive control — specialises `PlanningNeuron` with constrained optimisation |
| `CampaignPlanningNeuron` | 2 / 5 | Slow-loop production campaign optimisation |
| `MaintenanceSchedulingNeuron` | 2 / 10 | Schedules based on `DegradationSignal` + `MaintenanceWindowSignal` constraints |

### Layer 5 — action

| Class | Loop / Epoch | Role |
|---|---|---|
| `SafetyGateNeuron` | 1 / 1 | Specialises `HarmGateNeuron`; harm dimensions mapped to process-specific hazards |
| `ActuatorNeuron` | 1 / 1 | Writes to field; respects `OperatorOverrideSignal` — override always wins |
| `InterlockNeuron` | 1 / 1 | Hard-wired (via `EthicalPriorityNeuron` pattern) safety interlocks |

### Layer 7 — homeostasis / monitoring

| Class | Loop / Epoch | Role |
|---|---|---|
| `OscillationMonitorNeuron` | 1 / 2 | Specialises `OscillationBoundaryNeuron`; detects control-loop oscillation via ACF analysis |
| `EnergyAccountingNeuron` | 2 / 1 | Production-rate-aware energy attribution; economic layer of `EnergyNeuron` |

---

## 5. Interlock design — the critical safety primitive

Interlocks use the existing `EthicalPriorityNeuron` pattern with industrial specialisation:

- Hard constraints compiled at construction from the plant's safety requirements specification (SRS).
- Immutable by any runtime signal, including operator override. Operator override applies to *regulatory* control, not safety interlocks — this is standard industrial practice and the framework already matches it.
- Every interlock trip emits `InterlockSignal` and `TransparencyLogSignal`. No silent interlocks.
- `InterlockNeuron` has direct-to-actuator authority via `MotorCommandSignal` that bypasses `PlanningNeuron` and `ActionSelectionNeuron`. **This is the only permitted bypass in the whole architecture,** and only because it is safety-direction-only (always fail-safe).

**Architectural rule, inherited and reinforced:** an interlock can trip a process to safe state; it can never start a process or change a setpoint to a more aggressive value. Safety authority is asymmetric — safety actions that reduce hazard are permitted; any action that could increase hazard must still pass through the full `HarmGateNeuron` chain.

---

## 6. Oscillation damping — the headline feature

Cascaded control loops oscillate. Tuning is manual, time-consuming, and often wrong after process changes. `LoopCircuitBreakerNeuron`'s graduated interventions apply directly:

| Severity | Intervention | Industrial meaning |
|---|---|---|
| < 0.30 | `SCALE_WEIGHTS` | Reduce controller gain on suspected loop by 20% temporarily |
| 0.30 – 0.60 | `INJECT_INHIBITION` | Add derivative filter; reduce integrator windup |
| 0.60 – 0.85 | `BREAK_CONNECTION` | Temporarily open cascade — inner loop goes to manual with last-good setpoint |
| ≥ 0.85 | `QUARANTINE_NEURON` | Drop suspect loop to manual; alert control engineer |
| All of the above | `LoopRecoverySignal` | Restore original tuning after `durationTicks` — never permanent |

This alone is a publishable industrial control contribution — automated, reversible, auditable oscillation management.

---

## 7. Digital twin posture

The running jneopallium instance *is* the digital twin. `ConsequenceModelNeuron` forward-simulates using `ProcessModelNeuron` to evaluate candidate actions — this is the canonical digital-twin use case (predict before act).

- **Shadow mode:** set `safety.mode: shadow`. `ActuatorNeuron` emits `MotorCommandSignal(execute=false)` only; no field writes. Used for commissioning and for validating updated models.
- **Advisory mode:** `execute=true` gated by operator confirmation, same as clinical use case.
- **Autonomous mode:** `execute=true` released after `HarmGateNeuron` pass. Reserved for well-characterised, low-consequence loops.

Mode is per-loop, not global — a plant can run 90% of loops autonomous, 9% advisory, 1% shadow simultaneously.

---

## 8. Embodiment module integration

The embodiment module from the model-extensions CLAUDE.md is highly relevant here — industrial equipment *is* the body:

- `EfferenceCopySignal` on every `ActuatorCommandSignal` enables reafference-based fault detection: commanded valve 80% open but flow sensor says 30% → `ReafferenceComparatorNeuron` emits a `HarmFeedbackSignal`. This is mechanical-fault detection for free.
- `BodySchemaNeuron` tracks equipment configuration — unit out of service, redundant pump promoted. The digital twin stays synchronised with physical reality without manual intervention.
- `ToolIncorporationNeuron` models temporary connections (bypass hoses, mobile equipment skids) as body-schema extensions.

---

## 9. Configuration

```yaml
industrial:
  enabled: true
  tick-rate-hz: 100
  field-protocols:
    - opc-ua: "opc.tcp://plant-server:4840"
    - modbus: "tcp://..."
    - mqtt-sparkplug: "tcp://..."
  isa-95-level: 2                # process control layer
  safety:
    mode: advisory               # shadow | advisory | autonomous
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
    hash-chain-enabled: true     # tamper-evident log
    retention-days: 2555         # 7 years
  operator-override:
    always-honoured: true
    lockout-during-emergency-only: true
```

---

## 10. Validation criteria

Before deployment:

1. **Hazard and operability (HAZOP).** Every `EthicalPriorityNeuron` interlock traced to an SRS entry and exercised via fault-injection test.
2. **Loop tuning regression.** Baseline loops vs. jneopallium-managed loops across 30+ step tests. Integral absolute error (IAE) and overshoot must be within ±10% of baseline on normal conditions and better under disturbance.
3. **Oscillation recovery.** Induce oscillation on 20 test loops; `LoopCircuitBreakerNeuron` must recover all within `max-duration-ticks` without operator intervention.
4. **Operator override fidelity.** Inject `OperatorOverrideSignal` during every operational mode; must take effect within one tick.
5. **Shadow-mode model accuracy.** Before switching any loop to autonomous, require 30 days of shadow-mode operation with RMS prediction error below configured threshold.
6. **Tamper-evident audit.** Verify hash chain of `TransparencyLogSignal` output; any gap or mismatch blocks deployment.

---

## 11. Deployment topology

- **Edge node** per process unit — cluster-gRPC mode for deterministic latency. FPGA deployment (already a stated jneopallium target) is appropriate for critical fast-loop regulation.
- **Supervisory node** per area — consumes `EfficiencySignal`, `BatchStateSignal`, coordinates cross-unit optimisation.
- **Plant historian** as primary `IOutputAggregator` — all `MeasurementSignal` and `TransparencyLogSignal` persisted.
- **Control engineer workstation** as human-interface — alarms, shadow-mode comparisons, override capability.
- **LLM integration** (optional) isolated to engineering network only — used for operating-procedure lookup and root-cause analysis assistance, never in-the-loop on real-time control. `LLMVerificationNeuron` cross-references against the plant's own operating procedures before any content reaches operators.

---

## 12. Regulatory posture

- **IEC 61508** — functional safety. The interlock subsystem targets SIL 2 or SIL 3 depending on process; demonstrate independence from the control subsystem by physical/network segregation.
- **IEC 62443** — industrial cybersecurity. Reuse the cybersecurity use case's architecture on the OT network boundary.
- **ISA-95** — enterprise-control integration. This framework sits at ISA-95 level 2 (process control); integrate with MES (level 3) via standard interfaces, not jneopallium signals.
- **21 CFR Part 11** (pharma) — tamper-evident audit trail required; the hash-chained `TransparencyLogSignal` output satisfies this.
- Autonomous operation requires a safety case document — `TransparencyLogSignal` history plus shadow-mode validation is the evidence base.

---

## 13. Out of scope

- Direct replacement of certified safety PLCs. The jneopallium safety layer *advises and audits*; the certified SIS retains final authority on SIL-rated functions.
- Cloud-round-trip control. All fast-loop control runs edge-local. Cloud is for historian, optimisation planning, and model updates only.
- Use of the affect, curiosity, or LLM modules in-loop. Not safety-certifiable. Use on engineering workstations only.
- Uncontrolled model updates. Model changes go through the same MOC (management of change) as any other control modification.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7).
- IEC 61508:2010 — Functional safety of electrical/electronic/programmable electronic safety-related systems.
- IEC 62443 — Industrial communication networks — IT security for networks and systems.
- ISA-95 / IEC 62264 — Enterprise-control system integration.
- ISA-18.2-2016 — Management of alarm systems for the process industries.
- Seborg, D.E., Edgar, T.F., Mellichamp, D.A., Doyle, F.J. (2016). *Process Dynamics and Control.* Wiley.
- Ziegler, J.G., Nichols, N.B. (1942). Optimum Settings for Automatic Controllers. *Trans. ASME.* — classical PID tuning baseline.
- Grieves, M., Vickers, J. (2017). Digital Twin: Mitigating Unpredictable, Undesirable Emergent Behavior in Complex Systems. *Transdisciplinary Perspectives on Complex Systems.*
