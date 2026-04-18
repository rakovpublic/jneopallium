# Use Case: Cybersecurity — Biological Immune System Analogue

> **Framework:** [jneopallium](https://github.com/rakovpublic/jneopallium) + autonomous-AI architecture.
> **Domain:** Endpoint / network detection and response (EDR/NDR), intrusion detection, threat hunting.
> **Why jneopallium fits:** The biological immune system is the closest natural analogue to a working cybersecurity architecture — innate + adaptive layers, signature-based + anomaly-based detection, memory of past encounters, tolerance of self, graduated response. The framework's temporary-quarantine-then-recovery pattern from `LoopCircuitBreakerNeuron` is already exactly the right primitive.

---

## 1. Problem framing

Modern security architectures need:

- **Innate detection** — fast, signature-based (IOCs, YARA, snort rules). Biological analogue: macrophages, neutrophils.
- **Adaptive detection** — slow, anomaly-based, learns per-environment baseline. Biological analogue: T-cells, B-cells.
- **Memory** — past attacks recalled faster. Biological analogue: memory B/T cells.
- **Self-tolerance** — do not attack legitimate traffic. Biological analogue: thymic negative selection.
- **Graduated response** — log → alert → rate-limit → quarantine → block. Biological analogue: inflammation staging.
- **Oscillation damping** — avoid alert storms, avoid flapping quarantines. This is exactly what `LoopCircuitBreakerNeuron` solves for neural runaway.

Conventional ML-based EDRs often conflate detection and response, making incidents hard to audit. jneopallium's separation of signal flow from neuron logic, plus mandatory `TransparencyLogSignal` on every decision, gives a clean audit path.

---

## 2. Mapping to core framework

| Immune concept | jneopallium primitive |
|---|---|
| Antigen presentation | `SensorySignal` (packet, syscall, log event) |
| Signature match | `FeatureDetectorNeuron` (fast / 1) |
| Anomaly detection | `PredictionErrorNeuron` on baseline model |
| T-cell response | `AnomalyDetectorNeuron` (slow / 2, specialised) |
| Memory B-cell | `LongTermMemoryNeuron` specialised on attack patterns |
| Innate inflammation | `NeuromodulatorSignal(DOPAMINE_analogue=alert_level)` broadcast |
| Quarantine | Reuse `LoopCircuitBreakerNeuron.QUARANTINE_NEURON` → `QuarantineEntityNeuron` |
| Self-tolerance | `EthicalPriorityNeuron` with allow-list constraints |
| Immune exhaustion | `EnergyNeuron` + `HomeostasisNeuron` prevent alert-fatigue flooding |

---

## 3. Domain-specific signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.security`

| Signal | Loop / Epoch | Payload |
|---|---|---|
| `PacketSignal` | 1 / 1 | `byte[] summary`, `NetworkTuple tuple` (src, dst, proto, ports), `long timestamp` |
| `SyscallSignal` | 1 / 1 | `int syscallNum`, `int pid`, `String procName`, `long[] args` |
| `LogEventSignal` | 1 / 2 | `String source`, `LogLevel level`, `Map<String,String> fields`, `long timestamp` |
| `SignatureMatchSignal` | 1 / 1 | `String signatureId`, `String family`, `double confidence`, `String referenceIoc` |
| `AnomalyScoreSignal` | 1 / 2 | `String entityId`, `double deviationScore`, `List<String> contributingFeatures` |
| `ThreatHypothesisSignal` | 2 / 1 | `String hypothesisId`, `ThreatCategory cat`, `double posterior`, `List<String> evidenceIds` |
| `QuarantineRequestSignal` | 1 / 1 | `String entityId`, `EntityKind kind` (HOST, PROCESS, USER, CONNECTION), `int durationTicks`, `String reason` |
| `QuarantineLiftSignal` | 1 / 1 | `String entityId`, `String reason` |
| `InflammationBroadcastSignal` | 2 / 1 | `AlertLevel level`, `String region` (subnet/VLAN/tenant), `double magnitude` |
| `SelfToleranceSignal` | 2 / 5 | `String entityPattern`, `boolean allow` |
| `IncidentReportSignal` | 2 / 1 | `String incidentId`, `List<String> linkedEvents`, `Severity severity`, `String summary` |

---

## 4. Domain-specific neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.security`

### Layer 0 — ingestion

| Class | Loop / Epoch | Role |
|---|---|---|
| `PacketIngestNeuron` | 1 / 1 | Converts raw captures to `PacketSignal` (one per flow or per packet depending on rate) |
| `SyscallIngestNeuron` | 1 / 1 | eBPF/ETW-sourced `SyscallSignal` emission |
| `LogIngestNeuron` | 1 / 2 | Normalises heterogeneous logs (syslog, Windows Event, Cloud audit) into `LogEventSignal` |

### Layer 1 — innate (signature/rule)

| Class | Loop / Epoch | Role |
|---|---|---|
| `SignaturePatternNeuron` | 1 / 1 | Hyperscan/Aho-Corasick matcher over packet + log content; emits `SignatureMatchSignal` |
| `ProcessBehaviourNeuron` | 1 / 1 | Rule-based: forbidden syscall sequences (e.g., CreateRemoteThread + VirtualAllocEx + WriteProcessMemory) |
| `NetworkFlowNeuron` | 1 / 2 | Connection-feature extraction (bytes, duration, direction) |
| `InnateInterneuron` | 1 / 1 | Specialised `InhibitoryInterneuron` — suppresses known-benign signatures to prevent self-attack (see `SelfToleranceSignal`) |

### Layer 2 — adaptive (anomaly)

| Class | Loop / Epoch | Role |
|---|---|---|
| `AnomalyDetectorNeuron` | 1 / 2 | Per-entity baseline (user, host, service); `PredictionErrorNeuron` specialisation; emits `AnomalyScoreSignal` |
| `EntityBehaviourBaselineNeuron` | 2 / 3 | Slow-loop baseline update; only updates outside attack windows (gated by `InflammationBroadcastSignal`) |
| `BeaconingDetectorNeuron` | 1 / 3 | Periodicity analysis on network flows; detects C2 beacons |
| `LateralMovementNeuron` | 1 / 3 | Graph-based: unusual authentication patterns across hosts |

### Layer 3 — memory

| Class | Loop / Epoch | Role |
|---|---|---|
| `AttackMemoryNeuron` | 2 / 1 | Specialised `LongTermMemoryNeuron`; stores past attack patterns keyed by TTPs |
| `IncidentTimelineNeuron` | 2 / 2 | `EpisodicBufferNeuron` specialisation; binds related events into attack chains |

### Layer 4 — hypothesis / planning

| Class | Loop / Epoch | Role |
|---|---|---|
| `ThreatHypothesisNeuron` | 1 / 3 | MITRE ATT&CK-aligned hypotheses; Bayesian combination of `SignatureMatchSignal` + `AnomalyScoreSignal` |
| `ResponsePlanningNeuron` | 1 / 3 | Specialised `PlanningNeuron`; candidate responses: alert, rate-limit, quarantine, block, rollback |

### Layer 5 — response

| Class | Loop / Epoch | Role |
|---|---|---|
| `ResponseGateNeuron` | 1 / 1 | Specialised `HarmGateNeuron`; harm here is "false positive damages legitimate operation". SAFE → act; UNCERTAIN → alert only; HARMFUL → veto response |
| `QuarantineEntityNeuron` | 1 / 1 | Applies `QuarantineRequestSignal`; **always with duration** — never permanent. Quarantine lift is automatic |
| `RollbackNeuron` | 2 / 1 | On high-severity confirmed incidents: coordinates snapshot-based rollback where supported |

### Layer 7 — homeostasis

| Class | Loop / Epoch | Role |
|---|---|---|
| `AlertFatigueMonitorNeuron` | 2 / 2 | Tracks analyst acknowledgement rate on `IncidentReportSignal`; high FP rate → tighten `AnomalyScoreSignal` thresholds |
| `ImmuneExhaustionNeuron` | 2 / 1 | Specialises `EnergyNeuron`; prevents runaway rule evaluation during DDoS |

---

## 5. Self-tolerance and the "no friendly fire" invariant

`EthicalPriorityNeuron` (from the autonomous-AI module) gains immutable constraints at construction, injected from policy:

- Never quarantine assets tagged `critical=true` without a `high-severity + high-confidence` verdict.
- Never block traffic from an allow-listed identity provider or management plane.
- Never disable security tooling itself (anti-tamper).

Like all `EthicalPriorityNeuron` hard constraints, these are compiled at construction and cannot be modified by any runtime signal — this matters: a clever attacker who gains model-level access cannot coerce the system to stand down.

`SelfToleranceSignal` (slow loop, epoch 5) updates allow-lists. These are **separate** from the hard constraints: allow-lists can be refined over time; hard constraints cannot.

---

## 6. Graduated response — the key architectural win

Reuse `LoopCircuitBreakerNeuron`'s graduated intervention pattern directly:

| Severity | Response |
|---|---|
| < 0.30 | `IncidentReportSignal(severity=LOW)` — logged, not alerted |
| 0.30 – 0.60 | Alert to SOC console; increase logging verbosity on the entity |
| 0.60 – 0.85 | Rate-limit or network-segment the entity; `QuarantineRequestSignal` for a connection-level quarantine |
| ≥ 0.85 | Host-level `QuarantineRequestSignal`; revoke user session |
| Repeated > maxN | Escalate to human incident response; full forensics collection |

**Crucial rule inherited from the framework:** quarantine is *never permanent*. Every quarantine has `durationTicks`, after which `QuarantineLiftSignal` fires automatically unless independently reconfirmed by a still-active threat signal. This prevents the accumulated-lockout failure mode that plagues conventional EDR deployments.

---

## 7. Configuration

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
    baseline-window-ticks: 864000   # 1 day at 100Hz
    baseline-freeze-during-alert: true
    score-threshold-soft: 0.7
    score-threshold-hard: 0.9
  response:
    mode: enforcing      # or: monitor-only, alert-only
    quarantine-default-ticks: 18000  # 5 min at 100Hz
    quarantine-max-ticks: 360000     # 10 min
    rollback-enabled: false          # requires per-site enablement
  tolerance:
    allow-list-source: cmdb
    critical-asset-tag: "critical=true"
  homeostasis:
    max-alerts-per-min: 100
    adaptive-suppression: true
```

---

## 8. Validation criteria

Before production:

1. **Replay attack corpus.** Feed MITRE ATT&CK Evals traces, CALDERA-generated traces, and DARPA OpTC. Require ≥90% detection at documented stages, ≤1% false quarantine rate on benign baselines.
2. **Friendly-fire stress test.** Inject 1000 synthetic benign events designed to look anomalous (ops work at unusual hours, legitimate admin tools). Must not quarantine any `critical=true` asset.
3. **Alert-fatigue bounded.** Over a 24-hour simulated window, alert rate must stay under the configured `max-alerts-per-min` ceiling; if capacity is saturated, suppression must not drop high-severity events.
4. **Quarantine recovery.** 100% of quarantines must lift automatically at `durationTicks` expiry unless re-confirmed.
5. **Policy-violation audit.** `EthicalPriorityNeuron` constraints must block all attempts to quarantine allow-listed entities. Ship a dedicated red-team test asserting this.

---

## 9. Integration with LLM module (optional)

The `LLMKnowledgeNeuron` pair is valuable for threat intelligence enrichment — correlating an observed indicator with known campaigns:

- LLM queries triggered only on confirmed hypothesis with `posterior > 0.7`. Not on raw events (rate too high).
- `LLMVerificationNeuron` cross-references against `AttackMemoryNeuron` — LLM-claimed attribution must match at least one stored TTP profile to pass `APPLICABLE`.
- LLM output influences `IncidentReportSignal` narrative only; never triggers response action directly. Responses remain grounded in local evidence.

---

## 10. Deployment topology

- **Per-endpoint** instance in cluster-gRPC mode to keep fast-loop latency low.
- **Per-subnet** aggregator consuming `AnomalyScoreSignal` and `ThreatHypothesisSignal` for cross-host correlation.
- **SOC tier** as primary `IOutputAggregator` — consumes `IncidentReportSignal`.
- **SIEM integration** as secondary output — every `TransparencyLogSignal` ships to long-term storage for forensics.
- **Threat-intel LLM** isolated in a separate network segment; no inbound traffic from monitored network.

---

## 11. Out of scope

- Offensive capabilities (counter-attack, hack-back). The framework is defensive-only.
- Training data exfiltration from monitored systems without explicit legal basis.
- Full packet capture to disk — volume intractable; use metadata + selective capture triggered by `ThreatHypothesisSignal`.
- Use of the affect or curiosity modules — neither has a defensible security role and both introduce unaudited variance.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7).
- Forrest, S., Perelson, A.S., Allen, L., Cherukuri, R. (1994). Self-Nonself Discrimination in a Computer. *Proc. IEEE Symposium on Security and Privacy.*
- De Castro, L.N., Timmis, J. (2002). *Artificial Immune Systems: A New Computational Intelligence Approach.* Springer.
- MITRE ATT&CK Framework. https://attack.mitre.org/
- Greenberg, A. (2019). *Sandworm.* Doubleday. — case study in graduated response failure modes.
- Nygard, M. (2018). *Release It!* Pragmatic Bookshelf. — circuit-breaker patterns underpinning the quarantine duration design.
