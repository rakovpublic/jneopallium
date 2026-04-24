# Cybersecurity — Biological Immune System Analogue

> Status: implementation of [`use-case-cybersecurity-immune.md`](../../use-case-cybersecurity-immune.md) for [jneopallium](https://github.com/rakovpublic/jneopallium).
> License: BSD 3-Clause.

---

## Abstract

The cybersecurity module turns jneopallium into an EDR/NDR-style
detection-and-response platform that borrows its architecture from the
biological immune system. Innate signature detection plays the role of
macrophages and neutrophils; adaptive anomaly detection plays the role
of T-cells; a memory neuron plays the role of memory B-cells; a hard
self-tolerance gate plays the role of thymic negative selection; and
graduated response — log → alert → rate-limit → quarantine → block —
mirrors inflammation staging. Quarantine is **never permanent** by
construction: every request carries a positive duration and an
automatic lift signal fires at expiry unless independently
reconfirmed.

## Design Principles

1. **Typed timescales.** Packets and syscalls are fast-loop 1/1; logs
   1/2; anomaly scores 1/2; hypotheses 2/1; incident reports 2/1;
   soft allow-list 2/5. The scheduler places each stream on its
   natural cadence so fast-loop latency for detection isn't dragged
   down by slow-loop housekeeping.
2. **Processors are stateless, parameterised by interfaces.** Every
   `ISignalProcessor` in
   `worker/signalprocessor/impl/security` declares its neuron
   dependency as an `I<Neuron>` interface from
   `worker/net/neuron/impl/security`. Concrete neurons can be swapped
   per deployment without touching the processor.
3. **Graduated response.** `ResponsePlanningNeuron.band(posterior)`
   maps the posterior to one of `LOG / ALERT / CONNECTION_QUARANTINE /
   HOST_QUARANTINE / ESCALATE`. Quarantine requests include an
   explicit duration; `QuarantineEntityNeuron.tick(...)` reaps
   expired entries and emits lift signals automatically.
4. **Hard vs. soft tolerance.** `ResponseGateNeuron` owns the
   constructor-time hard allow-list plus the critical-asset list;
   neither is runtime-mutable through a signal. `InnateInterneuron`
   owns the soft allow-list that `SelfToleranceSignal` can update at
   runtime. An attacker who compromises signal flow can at most
   unblock a previously-trusted pattern — never stand down the hard
   constraints.
5. **Homeostasis.** `AlertFatigueMonitorNeuron` exposes a threshold
   multiplier that anomaly detectors apply when the false-positive
   rate drifts up. `ImmuneExhaustionNeuron` acts as a token-bucket on
   rule evaluation so a DDoS-style flood cannot starve downstream
   stages.

## Signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.security`.

| Signal | Loop/Epoch | Notes |
|---|---|---|
| `PacketSignal` | 1/1 | byte summary + `NetworkTuple` + timestamp |
| `SyscallSignal` | 1/1 | syscall number, pid, proc name, args |
| `LogEventSignal` | 1/2 | source, `LogLevel`, field map |
| `SignatureMatchSignal` | 1/1 | id, family, confidence, IoC |
| `AnomalyScoreSignal` | 1/2 | entity id, deviation, contributing features |
| `ThreatHypothesisSignal` | 2/1 | `ThreatCategory`, posterior, evidence |
| `QuarantineRequestSignal` | 1/1 | entity id, `EntityKind`, **positive** duration |
| `QuarantineLiftSignal` | 1/1 | entity id, reason |
| `InflammationBroadcastSignal` | 2/1 | `AlertLevel`, region, magnitude |
| `SelfToleranceSignal` | 2/5 | soft allow-list add / revoke |
| `IncidentReportSignal` | 2/1 | id, linked events, `Severity`, summary |

## Neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.security`.

Every concrete class implements a matching `I<Neuron>` interface so
processors never depend on the concrete type.

### Layer 0 — ingestion
- `PacketIngestNeuron` / `IPacketIngestNeuron` — rate-limited per-second
  bucket.
- `SyscallIngestNeuron` / `ISyscallIngestNeuron`.
- `LogIngestNeuron` / `ILogIngestNeuron`.

### Layer 1 — innate
- `SignaturePatternNeuron` / `ISignaturePatternNeuron` — substring
  matcher (plug Hyperscan behind the interface in prod).
- `ProcessBehaviourNeuron` / `IProcessBehaviourNeuron` — forbidden
  syscall-sequence matcher.
- `NetworkFlowNeuron` / `INetworkFlowNeuron` — per-5-tuple bytes /
  packets.
- `InnateInterneuron` / `IInnateInterneuron` — soft allow-list filter.

### Layer 2 — adaptive
- `AnomalyDetectorNeuron` / `IAnomalyDetectorNeuron` — normalised L2
  deviation from baseline, with soft / hard thresholds.
- `EntityBehaviourBaselineNeuron` / `IEntityBehaviourBaselineNeuron` —
  EWMA baseline per entity; **frozen** when inflammation is above
  WATCH so attack-window traffic isn't absorbed into the baseline.
- `BeaconingDetectorNeuron` / `IBeaconingDetectorNeuron` — low-CV
  inter-arrival detector.
- `LateralMovementNeuron` / `ILateralMovementNeuron` — fanout of
  authentications per user.

### Layer 3 — memory
- `AttackMemoryNeuron` / `IAttackMemoryNeuron` — campaign → TTP set.
- `IncidentTimelineNeuron` / `IIncidentTimelineNeuron` — evidence
  binding.

### Layer 4 — hypothesis / planning
- `ThreatHypothesisNeuron` / `IThreatHypothesisNeuron` — Bayesian
  combination of signature + anomaly evidence.
- `ResponsePlanningNeuron` / `IResponsePlanningNeuron` — graduated
  response bands.

### Layer 5 — response
- `ResponseGateNeuron` / `IResponseGateNeuron` — hard allow-list +
  critical-asset protection + mode enforcement.
- `QuarantineEntityNeuron` / `IQuarantineEntityNeuron` — positive
  duration only, automatic lift.
- `RollbackNeuron` / `IRollbackNeuron` — opt-in snapshot restore.

### Layer 7 — homeostasis
- `AlertFatigueMonitorNeuron` / `IAlertFatigueMonitorNeuron`.
- `ImmuneExhaustionNeuron` / `IImmuneExhaustionNeuron`.

## Processors

Package: `com.rakovpublic.jneuropallium.worker.signalprocessor.impl.security`.

| Signal | Processor(s) |
|---|---|
| `PacketSignal` | `PacketSignatureProcessor`, `PacketFlowProcessor` |
| `SyscallSignal` | `SyscallBehaviourProcessor` |
| `LogEventSignal` | `LogSignatureProcessor` |
| `SignatureMatchSignal` | `SignatureToleranceProcessor`, `SignatureHypothesisProcessor` |
| `AnomalyScoreSignal` | `AnomalyHypothesisProcessor` |
| `ThreatHypothesisSignal` | `HypothesisResponseProcessor` |
| `QuarantineRequestSignal` | `QuarantineGateProcessor`, `QuarantineApplyProcessor` |
| `QuarantineLiftSignal` | `QuarantineLiftProcessor` |
| `InflammationBroadcastSignal` | `InflammationBaselineProcessor` |
| `SelfToleranceSignal` | `SelfToleranceProcessor` |
| `IncidentReportSignal` | `IncidentFatigueProcessor`, `IncidentRollbackProcessor` |

Every processor's `getNeuronClass()` returns an interface. The
`SecurityModuleTest::processors_allInterfaceTyped` test asserts this
invariant.

## Integration

- `ResponseGateNeuron` specialises the role of `EthicalPriorityNeuron`
  for this module. The hard constraints — hard allow-list, critical
  asset list, mode — are set at construction / wiring time and are
  not mutated by any runtime signal.
- `QuarantineEntityNeuron` plays the role of the autonomous-AI
  framework's `LoopCircuitBreakerNeuron.QUARANTINE_NEURON` but
  specialised for security entities. The "never permanent" rule is a
  program-level invariant: `apply` refuses non-positive durations.
- `InflammationBroadcastSignal` freezes baseline adaptation at
  ALERT-ELEVATED and above; this keeps the adaptive layer from
  learning an attacker's behaviour as normal.
- Per spec §11, affect and curiosity modules must stay off.
  `SecurityConfig.isAffectDisabled()` and `isCuriosityDisabled()`
  report `true` as a program-level reminder.

## Configuration

`SecurityConfig` mirrors the YAML section from spec §7:

```yaml
security:
  enabled: true
  ingestion:
    packet-rate-limit: 1000000/s
    syscall-source: ebpf
    log-sources: [syslog, windows-event, cloudtrail]
  signatures:
    engine: hyperscan
    rulesets: [emerging-threats, sigma-converted, custom]
    update-interval-ticks: 36000
  anomaly:
    baseline-window-ticks: 864000
    baseline-freeze-during-alert: true
    score-threshold-soft: 0.7
    score-threshold-hard: 0.9
  response:
    mode: enforcing         # setter throws on unknown values
    quarantine-default-ticks: 18000
    quarantine-max-ticks: 360000
    rollback-enabled: false
  tolerance:
    allow-list-source: cmdb
    critical-asset-tag: "critical=true"
  homeostasis:
    max-alerts-per-min: 100
    adaptive-suppression: true
```

## Tests

`SecurityModuleTest` covers 35 cases: enum cardinalities, every
signal's `ProcessingFrequency`, per-layer behaviour (rate-limited
ingestion, signature match, forbidden syscall sequence, tolerance
filter, baseline freeze during inflammation, beaconing, lateral-
movement fanout, attack-memory lookup, incident timeline, Bayesian
hypothesis update, graduated-response bands, hard-allow / critical-
asset gate, quarantine auto-lift, rollback opt-in, alert-fatigue
multiplier, exhaustion budget, config validation, `NetworkTuple`
equality), plus:

- `processors_allInterfaceTyped` — every processor's neuron class is
  an interface.
- Per-processor smoke tests that walk a realistic signal path end-to-
  end (packet → signature → hypothesis → response → quarantine
  apply).

Full worker suite: all tests pass with the security module added, no
regressions.

## Validation (per spec §8)

1. *Replay attack corpus* — out of scope for unit tests; the ingestion
   interfaces accept any feed so a CALDERA / OpTC replay loop plugs
   in at deployment.
2. *Friendly-fire stress test* — asserted by `responseGate_*` and
   `processors_allInterfaceTyped` tests: critical assets are blocked
   below SAFE and hard-allow entities are always dropped.
3. *Alert-fatigue bounded* — `AlertFatigueMonitorNeuron.thresholdMultiplier()`
   rises monotonically with false-positive rate, giving downstream
   anomaly detectors a drop-in knob.
4. *Quarantine recovery* — `quarantine_neverPermanent_autoLift` test
   walks a full apply → tick → lift cycle.
5. *Policy-violation audit* — `responseGate_blocksHardAllow`,
   `responseGate_protectsCriticalAssetBelowSafe`,
   `responseGate_modeValidation`.

## Out of scope

Consistent with §11 of the use-case spec:

- Offensive capabilities (counter-attack, hack-back).
- Full packet capture.
- Affect, curiosity, or sleep modules.

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling:
  The Jneopallium Approach.* IJSR 13(7).
- Forrest, S., Perelson, A.S., Allen, L., Cherukuri, R. (1994).
  Self-Nonself Discrimination in a Computer. *IEEE S&P.*
- De Castro, L.N., Timmis, J. (2002). *Artificial Immune Systems.*
  Springer.
- MITRE ATT&CK Framework. https://attack.mitre.org/
- Nygard, M. (2018). *Release It!* Pragmatic Bookshelf.
