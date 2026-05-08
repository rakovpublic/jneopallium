# Jneopallium Bridge Framework — Universal Contract

> **Audience:** Claude Code, executing against a working clone of `https://github.com/rakovpublic/jneopallium`. This document is the **prerequisite** for every bridge spec in this directory. Read it once; the per-bridge specs reference it instead of restating it.
>
> **Goal:** Codify the rules and shared scaffolding that turn Jneopallium into a bridge-centred framework. A "bridge" is a typed adapter between an external real-world system and the Jneopallium signal pipeline:
>
> ```
> external system → bridge → Jneopallium typed signals → safety/planning/learning/audit modules → bridge output → external system
> ```
>
> Examples: OPC UA (industrial), Apache PLC4X (legacy PLCs), MQTT/Sparkplug (IIoT), FMI/FMU (simulation), ROS 2 (robotics), LSL (BCI), HL7 FHIR (clinical), DICOM (imaging), Apache Kafka (cyber/enterprise), OpenTelemetry (observability), Eclipse Ditto (digital twins), IEC 61850 (power grid), MAVLink (drones), CANopen (embedded), LTI/xAPI (adaptive tutoring).

---

## §0 The six ground rules — copy verbatim into every bridge spec

These rules are the **standard contract for every serious Jneopallium bridge**, lifted directly from the OPC UA bridge that established them. Code that violates any of them must not pass review.

1. **No raw write to a field actuator from neuron output.** Every effecting write goes through the chain `PlanningNeuron → SafetyGateNeuron → SafetyMode check → OperatorOverrideSignal check → <Bridge>OutputAggregator`. The aggregator rejects any actuating signal whose `execute=false` or whose loop/topic/channel is in shadow mode by config.
2. **Interlocks have direct authority.** When `InterlockSignal.tripped == true`, the aggregator MUST drive the bound output to its fail-safe value regardless of anything else in the same tick. This is the only permitted bypass.
3. **Operator override wins for regulatory control.** While an `OperatorOverrideSignal` is active for a tag, the aggregator does not write that tag from any neuron-derived signal for the duration of the override. Override does not cancel an interlock.
4. **Every write produces an audit record** — accepted, suppressed, clamped, rate-limited, or rejected. Audits are written to a local JSONL file and, optionally, mirrored to a bridge-specific audit channel (OPC UA audit node, Kafka topic, OTel span, FHIR Provenance resource, etc.).
5. **Quality propagates.** When the source protocol provides a quality/status code, derived signals carry that quality unmodified. Untrustworthy data is never silently promoted to "good".
6. **Wall-clock timestamps come from the source system**, not the bridge JVM. Use the protocol's source timestamp first, fall back to its server timestamp, and only use `System.currentTimeMillis()` if both are null.

A bridge that operates **read-only / advisory** still satisfies rules 4–6 trivially and may declare rules 1–3 N/A in its spec. A bridge that ever writes back to the source system MUST honour all six.

---

## 1. Bridge package convention

All new bridges go under a top-level `bridge` package:

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/<bridge-id>/
├── <Bridge>Config.java              ← Java record loaded from YAML (immutable)
├── <Bridge>ConfigLoader.java        ← Jackson-YAML loader (FAIL_ON_UNKNOWN_PROPERTIES = true)
├── <Bridge>NodeBinding.java         ← runtime binding: parsed protocol id + cached state
├── <Bridge>SignalMapper.java        ← pure functions: protocol DataType ↔ Jneopallium signal
├── <Bridge>ClientService.java       ← AutoCloseable connection lifecycle, reconnect, latest cache
├── <Bridge>MeasurementInput.java    ← IInitInput → MeasurementSignal-equivalent
├── <Bridge>EventInput.java          ← IInitInput → AlarmSignal/Event-equivalent (if applicable)
├── <Bridge>CommandOutputAggregator.java  ← IOutputAggregator (if writes are supported)
├── <Bridge>TransparencyLogOutput.java    ← append-only audit (or shared base)
└── package-info.java
```

The OPC UA bridge predates this convention and lives at `worker/net/neuron/impl/industrial/opcua/`. **Do not move it as part of any new bridge work** — that's a separate refactor PR. New bridges go in `worker.bridge.<id>`.

`<bridge-id>` values used by the specs in this directory: `plc4x`, `mqtt`, `fmi`, `ros2`, `lsl`, `fhir`, `dicom`, `kafka`, `otel`, `ditto`, `iec61850`, `mavlink`, `canopen`, `lti`.

---

## 2. Required interfaces every bridge implements

### 2.1 Reading from the external system → Jneopallium

Every read-side adapter implements the existing contract:

```java
package com.rakovpublic.jneuropallium.worker.net.signals.storage;

public interface IInitInput {
    List<IInputSignal> readSignals();
    String getName();
    HashMap<String, List<IResultSignal>> getDesiredResults();
    ProcessingFrequency getDefaultProcessingFrequency();
}
```

A `<Bridge>MeasurementInput` reads the latest cache from the connection service and emits typed `IInputSignal`s, one per binding. It does **not** block on the network — the connection service maintains the cache asynchronously. `readSignals()` is a snapshot.

### 2.2 Writing from Jneopallium → external system (when applicable)

Every write-side aggregator implements:

```java
package com.rakovpublic.jneuropallium.worker.application;

public interface IOutputAggregator {
    void save(List<IResult> results, long timestamp, long run, IContext context);
}
```

The aggregator's `save()` is the place where rules 1–4 are enforced, in this exact order:

1. Partition `results` into interlocks, overrides, and commands.
2. Write fail-safe values for tripped interlocks. **No vetoes apply.**
3. Record fresh `OperatorOverrideSignal`s into an `OverrideRegistry` keyed by tag, with TTL.
4. For each command:
   1. Resolve the binding by tag. If unknown → `REJECTED reason=UNKNOWN_TAG`.
   2. If the tag is under override → `OVERRIDE_HOLD`.
   3. Resolve effective `SafetyMode` for the loop. `SHADOW` → `REJECTED reason=SHADOW_MODE`. `ADVISORY` → `REJECTED reason=ADVISORY_HOLD` unless the command carries `execute=true` AND a separate operator-confirmation flag.
   4. Clamp by `[minClampValue, maxClampValue]` if configured.
   5. Rate-limit by `rampRateMaxPerSec * dt`.
   6. Diff-suppress writes that are within ε of the last applied value within 5 s.
   7. Issue the protocol-specific write through the connection service.
   8. Audit `APPLIED` (with reason if modified), `FAILED` (with status), or whatever final verdict applies.

This algorithm is **identical across bridges**. A common abstract base class is the right factoring; see §6.

### 2.3 Lifecycle

Both `<Bridge>ClientService` and `<Bridge>TransparencyLogOutput` implement `AutoCloseable`. Bootstrap programs always wrap them in try-with-resources. Failing to close leaks a network resource on the remote system.

Reconnect is exponential backoff capped at 30 s. After reconnect:
- Latest-value cache is **dropped** (the world may have changed).
- An advisory `AlarmSignal` (or domain equivalent) with condition `BRIDGE_RECONNECTED` is emitted.
- Buffered writes are **never silently replayed**.

---

## 3. Configuration

All bridges use YAML loaded through Jackson with `FAIL_ON_UNKNOWN_PROPERTIES = true`. A typo'd field is a safety incident, not a silent fallback.

Every bridge config has at minimum:

```yaml
connection:        # how to talk to the external system
  ...              # protocol-specific
security:          # auth + transport security
  ...              # protocol-specific
reads:             # subscription / poll bindings → input signals
  - bindingId: <stable-id>
    sourceId:  <protocol-native-id>
    signalTag: <ISA-95-style tag emitted on signals>
    direction: READ
writes:            # write bindings → output target (only if bridge supports writes)
  - bindingId: <stable-id>
    sourceId:  <protocol-native-id>
    signalTag: <ISA-95-style tag>
    direction: WRITE
    failSafeValue:        <value written on interlock trip>
    rampRateMaxPerSec:    <max change/sec; null = no limit>
    minClampValue:        <hard lower clamp>
    maxClampValue:        <hard upper clamp>
events:            # event/alarm subscriptions (if applicable)
  - ...
audit:
  localAuditFile: "/var/log/jneopallium/<bridge>-audit.jsonl"
  externalAuditTarget: <protocol-specific, optional>
  writeRejectedToAudit: true
perTagSafetyMode:  # SHADOW / ADVISORY / AUTONOMOUS per bindingId
  <id>: SHADOW
tickInterval:      "PT0.25S"
```

Per-bridge specs override `connection`/`security` shape and rename `reads` / `writes` / `events` to domain-natural terms (e.g. `topics` for MQTT, `nodes` for OPC UA, `services` for ROS 2).

Hot-reload is **not supported**. Changes follow a Management of Change procedure that mandates a controlled restart.

---

## 4. Audit schema (universal)

One JSON object per line. Stable keys for SIEM/historian ingestion.

```json
{
  "ts":         1740000000000,
  "run":        12345,
  "bridge":     "opcua",
  "verdict":    "APPLIED" | "REJECTED" | "INTERLOCK_TRIP" | "OVERRIDE_HOLD" | "FAILED",
  "loopId":    "FIC-101",
  "tag":       "PLANT.FIC101.SP",
  "proposed":   47.3,
  "effective":  45.0,
  "reason":    "RATE_LIMITED",
  "safetyMode": "AUTONOMOUS",
  "evidenceNeurons": ["Setpoint-12", "MPC-3"]
}
```

`verdict` values:
- `APPLIED` — write succeeded at the protocol layer.
- `REJECTED` — the aggregator refused; `reason` ∈ `{UNKNOWN_TAG, INTERLOCK_HOLD, OVERRIDE_HOLD, SHADOW_MODE, ADVISORY_HOLD, EXCEPTION:*}`.
- `INTERLOCK_TRIP` — fail-safe written because of a tripped interlock.
- `OVERRIDE_HOLD` — operator override active; nothing written.
- `FAILED` — protocol-level write returned a non-good status or threw.

`reason` for `APPLIED`: `RATE_LIMITED`, `CLAMPED_HIGH`, `CLAMPED_LOW`, `DIFF_SUPPRESSED`, or empty when applied verbatim.

---

## 5. Universal acceptance scenarios

Every bridge integration test plan **must** include scenarios S1–S6 below. Per-bridge specs add domain-specific scenarios numbered from S7.

| # | Scenario | Setup | Expected | Audit |
|---|----------|-------|----------|-------|
| **S1** | Pure read | Bridge connects, no writes configured, at least one binding has a value tick | Within 2 s, the input emits a typed signal with the source-system value | No command audit entries |
| **S2** | Bad quality propagates | Source returns an error/uncertain quality on a tag | Emitted signal carries `Quality.BAD` or domain equivalent; value passed through unchanged | None |
| **S3** | SHADOW mode rejects writes | A loop is configured `SHADOW`. Aggregator receives a command for it | No write at the protocol layer | One audit `verdict=REJECTED reason=SHADOW_MODE` |
| **S4** | Reconnect after disconnect | Server killed, restored after 3 s | Bridge reconnects with backoff. Pre-disconnect commands NOT replayed; an advisory event emitted | Reconnection event |
| **S5** | Audit failure isolation | Local audit file made unwritable, attempt apply | Apply still succeeds at the protocol layer | Stderr warning; bridge degraded but functional |
| **S6** | Unknown tag rejected | Aggregator receives a command with a tag not in the binding map | Skipped | One audit `verdict=REJECTED reason=UNKNOWN_TAG` |

Bridges that don't write skip S3 and S6. Bridges with no quality concept skip S2 and document why.

---

## 6. Recommended shared abstractions

To prevent the per-bridge aggregators from each re-implementing the §2.2 algorithm, provide a small base under `worker.bridge.common`:

```java
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/common/
├── AbstractBridgeOutputAggregator.java   ← template-method pattern for §2.2
├── OverrideRegistry.java                  ← TTL-keyed override map
├── BridgeAuditRecord.java                 ← shared JSON schema
├── AbstractBridgeAuditOutput.java         ← single-writer JSONL append
├── BridgeReconnectPolicy.java             ← exponential backoff, capped
├── BridgeBindingDirection.java            ← READ / WRITE / BOTH enum
└── package-info.java
```

`AbstractBridgeOutputAggregator` exposes a single abstract method: `protected abstract WriteResult issueWrite(BindingT binding, double value)`. Each bridge implements it with the protocol-specific write call. Every other concern — clamping, rate-limiting, override registry, audit — is in the base.

The OPC UA bridge can be retrofitted to extend `AbstractBridgeOutputAggregator` in a follow-up PR.

---

## 7. Phasing — universal pattern per bridge

Every bridge spec follows the same three phases:

| Phase | Goal |
|-------|------|
| **1** | **Read-only.** Bring the connection up, build the latest-value cache, emit signals into the network. No writes. Acceptance scenarios S1, S2, S4, S5. |
| **2** | **Advisory writes.** Implement `<Bridge>CommandOutputAggregator`. All loops start in `SHADOW`. After observation, promote chosen loops to `ADVISORY` (write to an advisory channel only, not the production target). Acceptance S3, S6 + bridge-specific. |
| **3** | **Autonomous writes (where appropriate).** Promote individual loops to `AUTONOMOUS` after a documented observation period. Some bridges (FHIR, DICOM, OpenTelemetry) **never** reach this phase by design. Their specs document the ceiling explicitly. |

Skipping phases is unsafe. The sequence is non-negotiable.

---

## 8. Signals — reuse the existing tree

A survey of `worker/src/main/java/com/rakovpublic/jneuropallium/worker/net/signals/impl/` shows that the signal tree is far richer than any single bridge needs. Per-bridge specs map to **existing** signal types wherever possible:

| Domain | Existing signals (selection) | Used by bridges |
|--------|----------------------------|-----------------|
| `industrial/` | `MeasurementSignal`, `SetpointSignal`, `ActuatorCommandSignal`, `AlarmSignal`, `InterlockSignal`, `OperatorOverrideSignal` | OPC UA, PLC4X, MQTT, FMI, IEC 61850 |
| `clinical/` | `VitalSignal`, `LabResultSignal`, `MedicationAdminSignal`, `DemographicSignal`, `DiagnosisHypothesisSignal`, `ImagingFindingSignal`, `TreatmentProposalSignal`, `WaveformSignal`, `AdverseEventAlertSignal`, `ClinicalVetoSignal` | FHIR, DICOM |
| `bci/` | `NeuralSpikeSignal`, `LFPSignal`, `ECoGSignal`, `IntentSignal`, `StimulationCommandSignal`, `DriftEstimateSignal`, `SeizureRiskSignal`, `AgencyLossSignal` | LSL |
| `security/` | `LogEventSignal`, `PacketSignal`, `AnomalyScoreSignal`, `IncidentReportSignal`, `SignatureMatchSignal`, `ThreatHypothesisSignal`, `SyscallSignal`, `QuarantineRequestSignal` | Kafka, OpenTelemetry |
| `swarm/` | `PeerObservationSignal`, `PeerStateSignal`, `ConsensusProposalSignal`, `ConsensusVoteSignal`, `FormationSignal`, `PheromoneSignal`, `TaskAnnouncementSignal`, `TaskBidSignal`, `TaskAssignmentSignal` | ROS 2, MAVLink |
| `embodiment/` | `ProprioceptiveSignal`, `EfferenceCopySignal`, `BodySchemaUpdateSignal`, `SensorimotorContingencySignal` | ROS 2, MAVLink, CANopen |
| `affect/` | `AffectStateSignal`, `InteroceptiveSignal`, `AppraisalSignal` | LSL, LTI/xAPI |
| `tutoring/` | `ResponseSignal`, `MasteryUpdateSignal`, `EngagementSignal`, `HintSignal`, `ItemPresentationSignal`, `InterventionSignal`, `ScaffoldingSignal`, `ContentRecommendationSignal` | LTI/xAPI |
| `ai/signals/fast/` | `SensorySignal`, `MotorCommandSignal`, `TransparencyLogSignal`, `HarmVetoSignal` | universal |

When a bridge spec says "**reuse existing**", it means *no new signal class* — the mapper just constructs an existing one.

When a bridge truly needs a new signal type, the spec states it explicitly and proposes a placement (typically a domain package alongside the existing ones).

---

## 9. Common dependencies

The bridge framework adds these dependencies to `worker/pom.xml` (managed in the parent BOM where possible):

```xml
<!-- YAML loader (already added by OPC UA work) -->
<dependency>
    <groupId>com.fasterxml.jackson.dataformat</groupId>
    <artifactId>jackson-dataformat-yaml</artifactId>
</dependency>
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
<!-- Reactive primitives used by reconnect logic -->
<dependency>
    <groupId>io.projectreactor</groupId>
    <artifactId>reactor-core</artifactId>
    <version>3.6.10</version>
</dependency>
```

Bridge-specific dependencies are declared in the per-bridge spec and managed in the parent BOM.

---

## 10. Definition of Done (universal)

A bridge implementation is "done" only when:

* [ ] Phase 1 (read-only) integration tests S1, S2, S4, S5 pass under `mvn verify`.
* [ ] Phase 2 (advisory) integration tests S3, S6 + bridge-specific scenarios pass.
* [ ] `<Bridge>ClientService` and `<Bridge>TransparencyLogOutput` are `AutoCloseable`; tests verify clean shutdown.
* [ ] Configuration loads via `<Bridge>ConfigLoader.load(Path)` with `FAIL_ON_UNKNOWN_PROPERTIES = true`. Unknown-key tests pass.
* [ ] No raw protocol write is reachable except through `<Bridge>CommandOutputAggregator`. Verified by package-private visibility on the write method of `<Bridge>ClientService`.
* [ ] Audit JSONL conforms to §4. One representative line is asserted in a unit test.
* [ ] A `docs/<bridge>-bridge.md` companion is committed with: one paragraph of domain context, the bridge YAML schema reference, a "manual demo" procedure, and a regulatory-posture note.
* [ ] The bridge is listed in the bridge index in the project README.

---

## 11. Bridge index — priority and safety posture

| ID | Bridge | Domain | Spec file | Priority | Safety ceiling |
|----|--------|--------|-----------|----------|----------------|
| (existing) | OPC UA | industrial | (in main repo) | shipped | AUTONOMOUS (per-loop) |
| 01 | Apache PLC4X | legacy PLC | `01-PLC4X.md` | very high | AUTONOMOUS (per-loop) |
| 02 | MQTT + Sparkplug | IIoT | `02-MQTT-SPARKPLUG.md` | very high | ADVISORY |
| 03 | FMI / FMU | simulation | `03-FMI-FMU.md` | extremely high | AUTONOMOUS (sim only) |
| 04 | ROS 2 / DDS | robotics | `04-ROS2-DDS.md` | very high | ADVISORY initially |
| 05 | Lab Streaming Layer | BCI | `05-LSL.md` | high | ADVISORY (read-mostly) |
| 06 | HL7 FHIR | clinical | `06-FHIR.md` | high | ADVISORY (regulatory ceiling) |
| 07 | DICOM | medical imaging | `07-DICOM.md` | medium-high | READ-ONLY (context bridge) |
| 08 | Apache Kafka | enterprise/cyber | `08-KAFKA.md` | high | ADVISORY |
| 09 | OpenTelemetry | observability | `09-OPENTELEMETRY.md` | very high | EXPORT-ONLY (no writeback) |
| 10 | Eclipse Ditto | digital twins | `10-DITTO.md` | medium-high | ADVISORY |
| 11 | IEC 61850 | power grid | `11-IEC61850.md` | medium | READ-ONLY initially |
| 12 | MAVLink | drones | `12-MAVLINK.md` | medium-high | SIM-ONLY initially |
| 13 | CANopen | embedded | `13-CANOPEN.md` | medium | ADVISORY |
| 14 | LTI / xAPI | adaptive tutoring | `14-LTI-XAPI.md` | medium-high | ADVISORY |

The "safety ceiling" column is the **maximum** safety mode the bridge supports as a class. Even an `AUTONOMOUS`-capable bridge starts each loop in `SHADOW`. A bridge whose ceiling is below `AUTONOMOUS` is structurally prevented (in code, not in policy) from issuing writes that affect human well-being without external confirmation.

---

## 12. References

* OPC UA bridge spec — the prototype that established §0–§7 (`JNEOPALLIUM_OPCUA_INTEGRATION.md` in the main repo).
* `IInitInput` — `worker/src/main/java/com/rakovpublic/jneuropallium/worker/net/signals/storage/IInitInput.java`.
* `IOutputAggregator` — `worker/src/main/java/com/rakovpublic/jneuropallium/worker/application/IOutputAggregator.java`.
* `KafkaInitInput` — closest existing analogue for any new `<Bridge>MeasurementInput`. Lives at `worker/src/main/java/com/rakovpublic/jneuropallium/worker/net/signals/storage/kafka/`.
