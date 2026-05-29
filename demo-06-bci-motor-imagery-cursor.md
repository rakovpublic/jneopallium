# Demo 06 — BCI motor-imagery cursor (Lab Streaming Layer, ADVISORY)

> Bridge: **Lab Streaming Layer (LSL)** ([doc](../lsl-bridge.md)) ·
> Module: **[bci](../modules/bci.md)** ·
> Safety ceiling: **ADVISORY (read-mostly)** ·
> External system: an LSL EEG stream — a **synthetic outlet** (BrainFlow's
> synthetic board bridged to LSL, or a `pylsl` example outlet) is enough; the
> bridge also ships an in-memory transport.
>
> Status: the LSL bridge is **spec + reference implementation** (README bridge
> index: "spec"); this demo runs against a synthetic LSL outlet so no hardware
> or wet electrodes are required.

A neuro-interface demo. EEG band-power features are decoded into a continuous
2-D cursor velocity (motor imagery: left/right hand, feet, rest). The pipeline is
**read-mostly** — it consumes neural data and emits an advisory control intent; a
stimulation safety gate exists in the module but stays inert because EEG is
input-only. A seizure watchdog runs as a hard safety monitor.

## Scenario

A subject performs motor-imagery tasks while EEG streams over LSL. The BCI
sub-net rejects artefacts (blinks, EMG, line noise), extracts sensorimotor-rhythm
band-power (µ/β), tracks signal drift, decodes a continuous cursor velocity with a
Kalman filter, and fuses it into a discrete intent. The decoded velocity is
published as an advisory control signal that a cursor application (or robotic
end-effector in a downstream demo) consumes. No electrical stimulation is ever
emitted in this configuration.

## What it demonstrates

| Feature | Where |
|---|---|
| LSL physiology/neural stream → typed signals | `LslPhysiologyInput`, `LslNeuralInput`, `LslSignalMapper`, `LslStreamBinding` |
| LSL marker stream for task labels / calibration | `LslMarkerInput` |
| Artefact rejection before decoding | `ArtefactRejectionNeuron` / `IArtefactRejectionNeuron` |
| Band-power / LFP feature extraction | `LFPExtractionNeuron` / `ILFPExtractionNeuron` |
| Non-stationarity / drift tracking | `DriftTrackerNeuron` / `IDriftTrackerNeuron` |
| Continuous decode (cursor velocity) | `KalmanDecoderNeuron` / `IKalmanDecoderNeuron` |
| Discrete intent fusion | `IntentFusionNeuron` / `IIntentFusionNeuron`, `IntentKind` |
| User-state monitoring (fatigue/engagement) | `UserStateNeuron` / `IUserStateNeuron` |
| Hard safety monitor | `SeizureWatchdogNeuron` / `ISeizureWatchdogNeuron` |
| Stimulation gate present but inert (read-mostly) | `StimulationSafetyGateNeuron` (no write path active) |
| Advisory-only intent emission + audit | `LslAdvisoryOutputAggregator`, `LslAuditOutput` |

## Architecture / data flow

```
 LSL outlet(s):  "EEG" (multi-channel float)   "Markers" (task labels)
        │ pylsl / liblsl resolve + pull
        ▼
  ┌──────────────────────────────────────────────┐
  │ LslClientService (LslTransport)                │
  └───┬───────────────────────┬───────────────────┘
      ▼                       ▼
 LslNeuralInput            LslMarkerInput
   → SensorySignal           → marker / calibration
   (EEG samples)
      │
      ▼
  ┌──────────────────────────────────────────────────────────┐
  │ BCI sub-net (worker/.../impl/bci):                        │
  │  ArtefactRejection → LFPExtraction (µ/β band-power)       │
  │     → DriftTracker (re-centre baseline)                   │
  │  KalmanDecoder → continuous cursor velocity (vx, vy)      │
  │  IntentFusion → IntentKind {LEFT,RIGHT,FEET,REST}         │
  │  UserState (fatigue/engagement)                           │
  │  SeizureWatchdog ──(hard monitor)──► halt + alarm         │
  │  StimulationSafetyGate  (inert: EEG is input-only)        │
  └───────────────────────────┬──────────────────────────────┘
                              ▼ List<IResult>
  ┌──────────────────────────────────────────────────────────┐
  │ LslAdvisoryOutputAggregator (advisory only)               │
  │  publishes an LSL outlet "CursorIntent" (vx,vy,intent)    │
  └───────────────────────────┬──────────────────────────────┘
                              ▼
        Cursor app / downstream consumer  +  LslAuditOutput
```

## Components used

* **Signals**: EEG samples → `SensorySignal` (fast loop); markers; advisory
  cursor-intent (vx, vy, `IntentKind`); `AlarmSignal` (watchdog).
* **Neurons** (`worker.net.neuron.impl.bci`): `ArtefactRejectionNeuron`,
  `LFPExtractionNeuron`, `DriftTrackerNeuron`, `KalmanDecoderNeuron`,
  `IntentFusionNeuron`, `UserStateNeuron`, `SeizureWatchdogNeuron`,
  `StimulationSafetyGateNeuron` (constructed but with no stimulation binding),
  `CalibrationSchedulerNeuron` (for the calibration step), `ActuatorNeuron`
  (advisory dispatch). `FeedbackModality` selects the user-feedback channel.
* **Bridge** (`worker.bridge.lsl`): `LslBridgeConfigLoader`, `LslClientService`,
  `LslTransport`, `LslSignalMapper`, `LslStreamBinding`, `LslNeuralInput`,
  `LslPhysiologyInput`, `LslMarkerInput`, `LslAdvisoryOutputAggregator`,
  `LslAuditOutput`.

## Configuration

`/tmp/demo06-bci.yaml`:

```yaml
connection:
  resolveTimeout: "PT5S"
streams:
  - bindingId: "EEG"
    type: "EEG"               # LSL stream type to resolve
    channels: 8
    sampleRateHz: 250
    signalTag: "BCI.EEG"
  - bindingId: "MARKERS"
    type: "Markers"
    signalTag: "BCI.MARKERS"

features:
  bands:                       # sensorimotor rhythm band-power
    mu:   [ 8.0, 13.0 ]
    beta: [ 13.0, 30.0 ]
  windowMs: 500
  hopMs: 100
decoder:
  kind: "kalman"
  intents: [ "LEFT", "RIGHT", "FEET", "REST" ]
safety:
  seizureWatchdog: true        # hard monitor; halts decode + raises alarm
  stimulation: false           # no stimulation outlet — read-mostly

advisory:
  outletName: "CursorIntent"   # advisory LSL outlet (vx, vy, intent)
audit:
  localAuditFile: "/tmp/jneopallium-demo06-audit.jsonl"
perTagSafetyMode:
  BCI.CURSOR.ADVISORY: ADVISORY
tickInterval: "PT0.02S"        # 50 Hz decode tick
```

## Run procedure

1. **Start a synthetic EEG outlet.** Either run BrainFlow's synthetic board and
   bridge it to LSL, or use a `pylsl` example outlet that pushes 8-channel float
   samples at 250 Hz with stream type `EEG`. Optionally start a `Markers` outlet
   that emits task cues (`LEFT`/`RIGHT`/`FEET`/`REST`) so the calibration and
   intent labels have ground truth.

2. **Build and wire the bridge + BCI sub-net:**

   ```java
   var cfg    = LslBridgeConfigLoader.load(Path.of("/tmp/demo06-bci.yaml"));
   var mapper = new LslSignalMapper(cfg);
   var audit  = new LslAuditOutput(Path.of(cfg.audit().localAuditFile()));
   var svc    = new LslClientService(cfg, mapper, audit);
   svc.start();

   var eegIn  = new LslNeuralInput("lsl-eeg", svc, List.of("EEG"));
   var mkIn   = new LslMarkerInput("lsl-mk", svc, List.of("MARKERS"));
   var agg    = new LslAdvisoryOutputAggregator(svc, audit);
   // build: artefactRejection → lfpExtraction → driftTracker →
   //        kalmanDecoder → intentFusion → userState → seizureWatchdog →
   //        stimulationSafetyGate(inert)
   ```

3. **Calibrate.** Run the cued blocks; `CalibrationSchedulerNeuron` collects
   labelled band-power windows and fits the decoder. Confirm marker alignment.

4. **Decode online.** Switch to free control. The advisory outlet `CursorIntent`
   should carry a continuous `(vx, vy)` that tracks the imagined movement on the
   synthetic data, plus the fused discrete `IntentKind`.

5. **Inject an artefact.** Add a large transient (simulated blink/EMG) on a
   frontal channel; `ArtefactRejectionNeuron` should gate that window out of the
   decode rather than letting it slew the cursor.

6. **Exercise the watchdog.** Feed a synthetic seizure-like rhythmic discharge;
   `SeizureWatchdogNeuron` halts the decode and raises a `HIGH` alarm. Because
   `stimulation: false`, no stimulation outlet exists to act on.

## Acceptance

* After calibration, decoded cursor velocity tracks the imagined movement on
  synthetic data and the discrete intent matches the cued label above chance.
* An injected artefact window is rejected and does not move the cursor.
* The seizure watchdog halts decoding and alarms on a seizure-like pattern.
* Only the advisory `CursorIntent` outlet is written; no stimulation path exists
  in this configuration; the audit log records decode/halt events.

## Safety / regulatory posture

Neural recording is sensitive and stimulation is the dangerous direction; this
demo is deliberately **read-mostly** — recording in, advisory intent out, and the
stimulation safety gate constructed but unbound. Where a deployment does drive
stimulation (a separate, higher-scrutiny configuration), `ChargeBalanceNeuron`,
`ThermalMonitorNeuron`, `PowerBudgetNeuron`, and `StimulationSafetyGateNeuron`
enforce charge-balance, thermal, and power limits before any output — see
[`../modules/bci.md`](../modules/bci.md). The seizure watchdog is a hard monitor,
not an advisory.
