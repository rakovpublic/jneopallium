# Use Case: Brain–Computer Interfaces & Neural Prosthetics

> **Framework:** [jneopallium](https://github.com/rakovpublic/jneopallium) + autonomous-AI architecture + embodiment module.
> **Domain:** Motor prostheses (limb replacement, exoskeleton control), sensory prostheses (retinal, cochlear implant extensions), assistive BCIs (speech/cursor/wheelchair), closed-loop neuromodulation (DBS, responsive neurostimulation).
> **Why jneopallium fits:** The framework's core premise — model biological neurons at a chosen granularity with typed signals — is exactly the abstraction layer missing in current BCI stacks. A prosthetic controller that exposes `SpikeSignal` and `MotorCommandSignal` at the same interface as biological neurons can participate naturally in a closed loop with real cortex. The embodiment module's `EfferenceCopyNeuron` is *specifically* what is missing in current myoelectric prostheses to restore the reafference-based sense of agency.

---

## 1. Problem framing

A clinically useful BCI has to solve, simultaneously:

- **Decoder** — map recorded neural activity (intracortical spikes, LFP, ECoG, EEG) to intent.
- **Effector driver** — translate intent into actuator commands (servos, stimulation patterns).
- **Closed-loop sensory feedback** — deliver tactile or proprioceptive information *back* to the user, so actions feel like their own.
- **Adaptation** — re-calibrate as electrode drift / cortical plasticity / hardware degradation accumulate.
- **Safety** — power limits, charge-balance in stimulation, seizure prevention, thermal limits.

Conventional BCIs decouple these into pipelines stitched together across hardware and software vendors. jneopallium's typed-signal architecture lets decoder, effector, feedback, and safety live in one network with clear boundaries — important for regulatory certification and for the closed-loop adaptation that distinguishes usable prostheses from demo-only ones.

---

## 2. Mapping to core framework

| BCI concept | jneopallium primitive |
|---|---|
| Neural recording | `SensorySignal` or custom `NeuralSpikeSignal` |
| Decoder | Chain of neurons from recording → intent |
| Intent | `MotorCommandSignal(execute=false)` |
| Effector output | `MotorCommandSignal(execute=true)` after safety gate |
| Stimulation pulse | Specialised `StimulationCommandSignal` |
| Sensory feedback | `SensoryFeedbackSignal` routed to afferent fibre stimulators |
| Efference copy | `EfferenceCopySignal` from the embodiment module |
| Reafference | Comparison against expected proprioception |
| Calibration | `HebbianLearningNeuron` + `MetaplasticityNeuron` on decoder weights |
| Charge-balance safety | Hard constraint in `EthicalPriorityNeuron` |
| Seizure watchdog | `OscillationBoundaryNeuron` on population activity |

---

## 3. Domain-specific signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.bci`

| Signal | Loop / Epoch | Payload |
|---|---|---|
| `NeuralSpikeSignal` | 1 / 1 | `int channelId`, `int unitId`, `double[] waveformSnippet`, `long timestampNs` |
| `LFPSignal` | 1 / 1 | `int channelId`, `double[] bandPowers` (delta, theta, alpha, beta, low-gamma, high-gamma), `long timestampNs` |
| `ECoGSignal` | 1 / 1 | `int electrodeId`, `double voltage`, `long timestampNs` |
| `IntentSignal` | 1 / 1 | `IntentKind kind` (REACH, GRASP, RELEASE, CURSOR_MOVE, SPEECH_PHONEME, etc.), `double[] parameters`, `double confidence` |
| `StimulationCommandSignal` | 1 / 1 | `int electrodeId`, `double amplitudeUA`, `double pulseWidthUS`, `double frequencyHz`, `int nPulses`, `PolarityPattern pattern` |
| `SensoryFeedbackSignal` | 1 / 1 | `FeedbackModality modality` (TACTILE, PROPRIOCEPTIVE, THERMAL, PAIN), `int afferentId`, `double intensity`, `double duration` |
| `ChargeAccumulationSignal` | 2 / 1 | `int electrodeId`, `double netChargeUC`, `double perPhaseChargeDensityUCm2` |
| `ThermalSignal` | 2 / 1 | `int sensorId`, `double temperatureC`, `double deltaFromBaseline` |
| `CalibrationSignal` | 2 / 3 | `String sessionId`, `CalibrationTarget target`, `double performanceScore` |
| `DriftEstimateSignal` | 2 / 5 | `int channelId`, `double drift`, `double snr` |
| `SeizureRiskSignal` | 1 / 1 | `double risk ∈ [0,1]`, `SeizureMarker marker`, `int region` |
| `AgencyLossSignal` | 1 / 2 | `double mismatchMagnitude`, `String modality` — emitted when efference copy ≠ reafference |

### Timing note

Neural events are sub-millisecond. The jneopallium fast loop runs at user-chosen frequency — for BCI this must be at least 1 kHz (tick = 1 ms), ideally 10 kHz for intracortical spike sorting. Document the tick-rate requirement per deployment and guard it at startup.

---

## 4. Domain-specific neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.bci`

### Layer 0 — front-end

| Class | Loop / Epoch | Role |
|---|---|---|
| `SpikeRecordingNeuron` | 1 / 1 | Reads from neural amplifier (Intan, Blackrock, Neuropixels); emits `NeuralSpikeSignal` |
| `SpikeSortingNeuron` | 1 / 1 | Online sorter (template matching, wavelet); assigns `unitId` |
| `LFPExtractionNeuron` | 1 / 1 | Multi-band-pass + Hilbert envelope; emits `LFPSignal` |
| `ArtefactRejectionNeuron` | 1 / 1 | Movement / stim-artefact detection; masks affected channels |

### Layer 1 — decoder

| Class | Loop / Epoch | Role |
|---|---|---|
| `FiringRateEstimatorNeuron` | 1 / 1 | Per-unit rolling rate estimate |
| `PopulationVectorNeuron` | 1 / 1 | Classic Georgopoulos-style population vector for motor decode |
| `KalmanDecoderNeuron` | 1 / 1 | State-space decoder for cursor / end-effector kinematics |
| `LatentDynamicsNeuron` | 1 / 2 | Slower manifold-based decoder (LFADS-style) for trajectory planning |
| `SpeechPhonemeDecoderNeuron` | 1 / 1 | For speech prostheses; emits `IntentSignal(kind=SPEECH_PHONEME)` |

### Layer 2 — intent integration

| Class | Loop / Epoch | Role |
|---|---|---|
| `IntentFusionNeuron` | 1 / 2 | Combines fast spike-based decode with slower LFP-based context |
| `UserStateNeuron` | 1 / 3 | Alert / fatigued / confused state from LFP bands and behavioural cues; modulates thresholds |

### Layer 3 — memory and adaptation

| Class | Loop / Epoch | Role |
|---|---|---|
| `DecoderWeightNeuron` | 2 / 3 | Decoder parameters as memory; Hebbian updates from user-acknowledged correct decodes |
| `DriftTrackerNeuron` | 2 / 5 | Per-channel SNR and drift; drives automatic channel exclusion |
| `PersonalMotorLexiconNeuron` | 2 / 1 | User-specific intent primitives — idiosyncratic gestures the user invents over time |

### Layer 4 — effector planning

| Class | Loop / Epoch | Role |
|---|---|---|
| `ProstheticPlanningNeuron` | 1 / 3 | Specialises `PlanningNeuron` for limb kinematics; candidate trajectories evaluated via forward model |
| `GripSelectionNeuron` | 1 / 2 | Power / pinch / lateral / tripod grip selection from `IntentSignal` context |

### Layer 5 — safety + actuation

| Class | Loop / Epoch | Role |
|---|---|---|
| `StimulationSafetyGateNeuron` | 1 / 1 | Specialises `HarmGateNeuron` for stimulation; hard limits on charge density, frequency, duty cycle |
| `ActuatorNeuron` | 1 / 1 | Emits `MotorCommandSignal(execute=true)` to servo/stimulator after gate |
| `ChargeBalanceNeuron` | 1 / 1 | Tracks per-electrode net charge; forces balanced biphasic pulses |
| `SeizureWatchdogNeuron` | 1 / 1 | Specialises `OscillationBoundaryNeuron`; detects pre-ictal patterns; emergency stops stimulation |

### Layer 7 — homeostasis

| Class | Loop / Epoch | Role |
|---|---|---|
| `ThermalMonitorNeuron` | 2 / 1 | Implant temperature tracking; throttles activity if above limits |
| `PowerBudgetNeuron` | 2 / 1 | Specialises `EnergyNeuron` — battery/inductive-link budget |
| `CalibrationSchedulerNeuron` | 2 / 3 | Detects decoder performance drift; schedules recalibration sessions |

---

## 5. Sensory feedback — the reason embodiment module matters

The defining difference between a working prosthesis and a demo is whether the user feels that the limb is theirs. Biology achieves this via reafference cancellation: the brain predicts the sensory consequence of a motor command (`EfferenceCopySignal`) and compares it with actual input (`ProprioceptiveSignal`).

`EfferenceCopyNeuron` (from the embodiment module) is integrated directly:

- Every `IntentSignal` → `MotorCommandSignal` path produces an `EfferenceCopySignal`.
- `SensoryFeedbackSignal` delivers a predicted sensory consequence back to the afferent nerve stimulator at the anticipated time.
- `ReafferenceComparatorNeuron` checks that the delivered feedback matches the efference copy within tolerance — a mismatch flags `AgencyLossSignal`, which is both a fault indicator (electrode problem, actuator problem) and a user-experience metric to optimise.

This is the difference between "the hand opens when I try" and "the hand is my hand."

---

## 6. Safety — stimulation limits as hard constraints

`EthicalPriorityNeuron` compiled at construction carries the following immutable constraints:

1. **Charge density limit** — Shannon criterion, typically ≤ 0.5 mC/cm² per phase on standard platinum microelectrodes. Breached → immediate veto.
2. **Net DC charge** — zero within each biphasic pulse; per-electrode running mean monitored by `ChargeBalanceNeuron`.
3. **Frequency × pulse-width safe operating area** — per electrode type; violations → veto regardless of decoder confidence.
4. **Thermal ceiling** — tissue heating above 1 °C sustained triggers cool-down; 2 °C sustained triggers shutdown.
5. **Seizure lockout** — if `SeizureRiskSignal > 0.8`, all stimulation commands vetoed for a configurable cool-down period.
6. **No modification of the safety layer itself** — same rule as the autonomous-AI module; inherited here without relaxation.

These are not parameters. They are compiled constants. A clever decoder weight update that would push stimulation into an unsafe regime is rejected by the gate, not by hopes of good weight-update behaviour.

---

## 7. Closed-loop neuromodulation (DBS / RNS)

For adaptive deep brain stimulation and responsive neurostimulation devices:

- `LFPExtractionNeuron` watches biomarker bands (e.g., beta power for Parkinson's tremor, gamma for epilepsy).
- `PlanningNeuron` proposes stimulation adjustments; `StimulationSafetyGateNeuron` vetoes unsafe ones.
- `HarmContextNeuron` tightens thresholds during sleep or when patient-reported distress is high.
- Transparency log captures every parameter change; regulatory submissions (FDA PMA for Class III) require this level of traceability and the framework provides it natively.

---

## 8. Configuration

```yaml
bci:
  enabled: true
  tick-rate-hz: 10000
  recording:
    device: blackrock-neural-signal-processor
    channels: 96
    sort-online: true
  decoder:
    primary: kalman
    secondary: lfads
    fusion-weights: [0.7, 0.3]
  feedback:
    enabled: true
    modalities: [tactile, proprioceptive]
    stimulator: "..."
  safety:
    charge-density-limit-uCcm2: 0.5
    net-dc-charge-tolerance-nC: 1
    frequency-hz-max: 300
    pulse-width-us-max: 300
    thermal-ceiling-deltaC: 1.0
    seizure-risk-threshold: 0.8
    seizure-lockout-seconds: 60
  adaptation:
    online-calibration: true
    calibration-session-minutes: 5
    drift-threshold-snr: 3.0
  power:
    battery-mAh: 500
    budget-enforcement: strict
  audit:
    log-every-stimulation: true
    retention-years: 10
  embodiment:
    enabled: true
    efference-copy-tolerance: 0.15
```

---

## 9. Validation criteria

Before any human trial:

1. **Bench validation of safety constants.** All hard constraints tested via fault injection; `EthicalPriorityNeuron` must block every out-of-bounds attempt.
2. **Charge balance verification.** Measure net DC charge at each electrode over extended simulated use; must stay within specification.
3. **Seizure lockout test.** Inject LFP patterns representative of pre-ictal activity; `SeizureWatchdogNeuron` must trigger within configured window.
4. **Decoder stability.** 90-day simulated drift run with synthetic recordings; decoder performance must degrade gracefully (no catastrophic failure) and `CalibrationSchedulerNeuron` must correctly flag when recalibration is needed.
5. **Reafference fidelity.** For sensory-feedback systems, mismatch between expected and delivered feedback must stay below `efference-copy-tolerance` during normal operation; exceeding → emit `AgencyLossSignal` and alert.
6. **Regulatory documentation.** `TransparencyLogSignal` output must cover every parameter change over the validation period; regulatory reviewer must be able to reconstruct device behaviour from logs alone.

Human trials require IRB approval separate from and independent of this framework's validation.

---

## 10. Deployment topology

- **Implant-side** — absolute minimum code; typically just recording and safety-critical stimulation clamping. FPGA-targeted deployment (a stated jneopallium capability) is appropriate.
- **Wearable processor** — primary jneopallium instance; decoding, planning, adaptation.
- **Clinician console** — consumes `TransparencyLogSignal`, calibration metrics, drift estimates. Adjustments go through a change-control workflow, not direct runtime writes.
- **Cloud** — optional; deidentified performance data for population-level decoder improvements (opt-in, with clear IRB basis).

Latency budget matters: intent → actuator ≤ 100 ms end-to-end is the usability ceiling; ≤ 30 ms is clinically excellent. Place decoder-to-effector on the same physical device.

---

## 11. Regulatory posture

- **FDA Class III** for implantable stimulators (PMA pathway). Class II for external assistive BCIs (510(k)).
- **IEC 60601-1** — basic safety of medical electrical equipment.
- **IEC 62304** — medical device software lifecycle. Framework's audit log satisfies a substantial portion of this.
- **ISO 14708** series — active implantable medical devices.
- Software changes post-approval go through a regulated change process. Decoder weight updates inside `MetaplasticityNeuron` are automatic within predefined parameter envelopes; structural changes (new neurons, new connections) require supplemental regulatory submission.

---

## 12. Out of scope

- "Enhancement" applications (beyond-baseline human capability). This spec targets restoration of lost function only; cognitive / mood enhancement is outside the therapeutic use case and raises distinct ethical considerations.
- Wireless power/data protocols — choose per-device; framework is transport-agnostic.
- Unsupervised autonomous adaptation of safety parameters. Safety constants are compiled; they do not adapt at runtime. Ever.
- Use of the affect module to model the user's emotional state beyond the coarse `UserStateNeuron`. Clinical emotion inference from neural signals is an active research area and is not production-ready.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7).
- Georgopoulos, A.P. et al. (1986). Neuronal population coding of movement direction. *Science* 233(4771), 1416–1419.
- Velliste, M. et al. (2008). Cortical control of a prosthetic arm for self-feeding. *Nature* 453, 1098–1101.
- Hochberg, L.R. et al. (2012). Reach and grasp by people with tetraplegia using a neurally controlled robotic arm. *Nature* 485, 372–375.
- Pandarinath, C. et al. (2018). Inferring single-trial neural population dynamics using sequential auto-encoders. *Nature Methods* 15, 805–815 — LFADS.
- Flesher, S.N. et al. (2021). A brain-computer interface that evokes tactile sensations improves robotic arm control. *Science* 372, 831–836.
- Shannon, R.V. (1992). A model of safe levels for electrical stimulation. *IEEE TBME* 39(4), 424–426.
- Cogan, S.F. (2008). Neural stimulation and recording electrodes. *Annual Review of Biomedical Engineering* 10, 275–309.
- Wolpert, D.M., Miall, R.C., Kawato, M. (1998). Internal models in the cerebellum. *Trends in Cognitive Sciences* 2(9), 338–347.
