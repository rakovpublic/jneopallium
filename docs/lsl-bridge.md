# Lab Streaming Layer (LSL) bridge

> Companion to [`05-LSL.md`](../05-LSL.md) (the spec) and
> [`00-FRAMEWORK.md`](../00-FRAMEWORK.md) (the universal contract). This
> file documents the LSL bridge's domain context, YAML schema, manual demo
> procedure, and regulatory posture — i.e. the §10 Definition-of-Done items
> that don't belong in the spec itself.

## Domain context

[Lab Streaming Layer](https://labstreaminglayer.org/) is the open-source
middleware ecosystem used in BCI and physiology research. It standardises
streaming, time-synchronisation, and recording of neural, physiological, and
behavioural data across heterogeneous acquisition devices (EEG amplifiers,
EMG, eye-trackers, motion-capture, custom hardware).

LSL is unusual: there is **no central broker**. Devices announce streams
via mDNS multicast; clients discover and pull them. The bridge is therefore
an LSL "inlet" client. The Java binding is `liblsl-Java`.

The bridge maps inbound LSL chunks to existing typed Jneopallium signals
(`LFPSignal`, `NeuralSpikeSignal`, `ECoGSignal`, `ProprioceptiveSignal`,
`InteroceptiveSignal`, `AppraisalSignal`, `ThermalSignal`,
`CalibrationSignal`) and publishes egress markers and numeric samples on
named outlets that downstream stimulator-driver software can subscribe to
(`Jneopallium-Stim-Advisory`, `Jneopallium-Intent`, `Jneopallium-Risk`).

## Components

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/lsl/
├── LslBridgeConfig.java               ← YAML record (immutable; AUTONOMOUS rejected)
├── LslBridgeConfigLoader.java         ← Jackson YAML loader, FAIL_ON_UNKNOWN
├── LslStreamBinding.java              ← inlet/outlet binding (BridgeBinding)
├── LslSignalMapper.java               ← LSL chunk ↔ typed signal (pure)
├── LslTransport.java                  ← Inlet / Outlet abstraction (test seam)
├── LslClientService.java              ← orchestrator: resolve, poll, ring buffer, events
├── LslNeuralInput.java                ← IInitInput → LFPSignal / NeuralSpikeSignal / ECoGSignal
├── LslPhysiologyInput.java            ← IInitInput → Interoceptive / Appraisal / Thermal / Proprioceptive
├── LslMarkerInput.java                ← IInitInput → CalibrationSignal + bridge events
├── LslAuditOutput.java                ← local JSONL audit
├── LslAdvisoryOutputAggregator.java   ← IOutputAggregator (Stim / Intent / Risk egress)
└── package-info.java
```

## YAML schema

Refer to 05-LSL.md §6 for the canonical example. Key points:

* `discovery.resolveTimeoutMs` — bridge fails fast if any
  `expectedStreams` entry is not present after the timeout (S7).
* `reads[].channels` — per-channel allow-list. Resolution against the
  inlet's reported channel layout fails fast on a missing label (§9 S10).
* `reads[].targetSignal` — one of `LFP`, `NEURAL_SPIKE`, `ECOG`,
  `EMG_PROPRIOCEPTIVE`, `INTEROCEPTIVE`, `APPRAISAL`, `THERMAL`,
  `CALIBRATION_MARKER`.
* `writes[].stimulationGated` — set on the stimulation-advisory binding
  so {@link LslAdvisoryOutputAggregator} routes
  `StimulationCommandSignal`s through it via the
  `StimulationSafetyGateNeuron`.
* `perTagSafetyMode` — per-binding safety mode. The LSL ceiling rejects
  `AUTONOMOUS` at config load (§3, §11 R3).

## Manual demo

Stand-up a synthetic LSL publisher with `pylsl`:

```bash
pip install pylsl
python -c "
import pylsl, time, math
info = pylsl.StreamInfo('OpenViBE-EEG-256Hz', 'EEG', 4, 256, 'float32', 'demo-eeg')
ch = info.desc().append_child('channels')
for label in ['Cz','Fz','Pz','Oz']:
    ch.append_child('channel').append_child_value('label', label)
outlet = pylsl.StreamOutlet(info)
t = 0
while True:
    sample = [math.sin(2*math.pi*10*t/256.0)] * 4
    outlet.push_sample(sample)
    t += 1
    time.sleep(1/256.0)
"
```

Then run a small bootstrap that wires `LslClientService` against a real
liblsl `LslTransport` implementation (out of scope — production wiring
constructs an `LiblslLslTransport` that delegates to
`edu.ucsd.sccn.LSL.StreamInlet`).

For development, the in-memory transport at
`worker/src/test/java/.../bridge/lsl/InMemoryLslTransport.java` can be
driven from a test or REPL without launching multicast or linking the
native binary.

## Regulatory posture

* **Read** — observational telemetry. Subject identifiers are pseudonymised
  by `LslSignalMapper.toCalibration` before any signal leaves the bridge
  (§10 R4). Marker text is hashed; the raw `subject_42_calibration_start`
  marker becomes the sessionId `MARKER-<hex>`.
* **Egress** — the bridge writes only to outlet topics that are themselves
  consumed by separately certified stimulator-driver software. There is
  no direct path from this bridge to electrodes (§3).
* **Safety ceiling** — `ADVISORY`. `AUTONOMOUS` is structurally rejected
  by the config-load validator. Every `StimulationCommandSignal` is
  routed through the existing `StimulationSafetyGateNeuron`; if the gate
  vetoes the command (Shannon criterion, charge balance, frequency SOA,
  seizure / thermal lockout) the outlet is not written and the audit
  records `verdict=REJECTED reason=GATE_VETO:<cause>`.
* **Privacy** — local-only, ephemeral storage. The audit JSONL is written
  to `audit.localAuditFile`. No cloud egress. Operator runbook MUST
  document consent capture (§10 R3).

## Acceptance scenarios

| # | Scenario | Source |
|---|----------|--------|
| S1 / S7 | Discover & bind, EEG → LFPSignal chunk | `LslBridgeIntegrationTest.s7_discoverAndBind_emitsLfpSignals` |
| S8 | Time correction applied within 2 ms | `s8_timeSyncAppliesTimeCorrection` |
| S9 | Stream disappears → `LSL_STREAM_LOST` alarm + cache stops updating | `s9_streamLostEmitsAlarm` |
| S10 | Channel mismatch → fail fast at start | `s10_channelMismatchFailsFast` |
| S11 | Outlet write after gate pass → `APPLIED` audit | `s11_outletWriteAfterSafetyGatePass` |
| S3 | SHADOW mode rejects writes | `s3_shadowModeRejectsStimulation` |
| S4 (eq.) | Reconnect drops cache + emits `BRIDGE_RECONNECTED` | `onReconnectedClearsCacheAndEmitsAdvisory` |
| S5 | Audit-failure isolation | `s5_auditFailureIsolated` |
| S6 | Unknown tag silently skipped (no Intent outlet → no write) | `s6_unknownTagSkippedSilently` |
| §0.2 | Interlock → fail-safe pushed regardless of mode | `interlockDrivesFailsafeOnRiskOutlet` |
| §0.3 | Operator override holds risk writes | `operatorOverrideHoldsRiskWrites` |
| §10 R4 | Marker pseudonymised in CalibrationSignal session id | `markerCueMatchesEmitsCalibrationSignalWithPseudonymisedId` |
| Gate veto | Stim command vetoed → `GATE_VETO` audit | `stimulationGateVetoRejectsCommand` |
| Mapping | HRV / Eye → Interoceptive / Appraisal | `hrvMapsToInteroceptiveSignal`, `eyeStreamMapsToAppraisalSignal` |
