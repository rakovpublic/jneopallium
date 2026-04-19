# BCI / Neural-Prosthetics Module

## Abstract

The `bci` module turns the jneopallium engine into a reference platform for
closed-loop brain-computer interfaces driving neural prosthetics, sensory
feedback, and assistive communication. It closes the loop from intracortical
or ECoG recording through online decoding (Georgopoulos population vector,
Kalman state-space, LFADS-style latent dynamics, speech-phoneme
classification) to safety-gated stimulation and prosthetic actuation. The
safety envelope is non-negotiable: every stimulation command is vetted
against the Shannon charge-density criterion, per-phase charge limits,
frequency / pulse-width bounds, charge-balance DC accumulation, seizure
lockout, and thermal shutdown.

## Design Principles

1. **Safety by construction** — no stimulation reaches the driver without
   passing `StimulationSafetyGateNeuron`. Shannon 0.5 µC/cm² per phase is
   the default ceiling; DC drift > 1 nC vetoes the electrode.
2. **Biologically grounded decoders** — population vector, Kalman, LFADS
   are the canonical intra-cortical decoders; speech phonemes follow the
   Moses / Willett 2021-2023 work.
3. **User ownership** — `PersonalMotorLexiconNeuron` lets the user invent
   personal gestures; `UserStateNeuron` classifies fatigue / confusion /
   distress and backs off stim when the user is not in nominal control.
4. **Transparency** — safety vetoes, seizure alarms, thermal shutdowns,
   calibration events are all observable signals; they must be routed to
   the existing `TransparencyLogSignal` bus for the oversight UI.
5. **Optional module** — `bci.enabled=false` keeps the module dormant; zero
   behavioural change to existing configurations.

## Signals

| Signal | Loop / Epoch | Role |
|---|---|---|
| `NeuralSpikeSignal` | 1 / 1 | Raw spike event per channel / unit |
| `LFPSignal` | 1 / 1 | Six-band LFP power |
| `ECoGSignal` | 1 / 1 | ECoG voltage sample |
| `IntentSignal` | 1 / 1 | Decoded user intent with confidence |
| `StimulationCommandSignal` | 1 / 1 | Requested pulse train to an electrode |
| `SensoryFeedbackSignal` | 1 / 1 | Synthetic afferent activation |
| `ChargeAccumulationSignal` | 2 / 1 | Net DC charge and density tracking |
| `ThermalSignal` | 2 / 1 | Implant / tissue temperature telemetry |
| `CalibrationSignal` | 2 / 3 | Session boundary + calibration target |
| `DriftEstimateSignal` | 2 / 5 | Per-channel waveform drift / SNR |
| `SeizureRiskSignal` | 1 / 1 | Pre-ictal / ictal risk + marker |
| `AgencyLossSignal` | 1 / 2 | Efference-vs-reafference mismatch alarm |

## Neurons

### Layer 0 — front-end
- `SpikeRecordingNeuron` — threshold-crossing detector.
- `SpikeSortingNeuron` — online template matching.
- `LFPExtractionNeuron` — six-band power extraction.
- `ArtefactRejectionNeuron` — amplitude-based channel masking.

### Layer 1 — decoders
- `FiringRateEstimatorNeuron` — exponential-moving-average per-unit rates.
- `PopulationVectorNeuron` — Georgopoulos 1986 rate-weighted PD sum.
- `KalmanDecoderNeuron` — 2-state (pos, vel) Kalman filter (Wu et al.
  2006).
- `LatentDynamicsNeuron` — LFADS-style manifold stand-in (Pandarinath
  et al. 2018).
- `SpeechPhonemeDecoderNeuron` — centroid classifier stand-in.

### Layer 2 — fusion
- `IntentFusionNeuron` — confidence-weighted spike/LFP blend.
- `UserStateNeuron` — alert / fatigued / confused / distressed.

### Layer 3 — adaptation
- `DecoderWeightNeuron` — online Hebbian weight update.
- `DriftTrackerNeuron` — rolling drift / SNR per channel.
- `PersonalMotorLexiconNeuron` — user-defined gesture memory.

### Layer 4 — planning
- `ProstheticPlanningNeuron` — joint-limited trajectory step.
- `GripSelectionNeuron` — power / pinch / lateral / tripod selection.

### Layer 5 — output + safety
- `StimulationSafetyGateNeuron` — Shannon / frequency / lockout gate.
- `ActuatorNeuron` — dispatch record after gate approval.
- `ChargeBalanceNeuron` — net DC charge tracker.
- `SeizureWatchdogNeuron` — pre-ictal LFP classifier.

### Layer 7 — supervision
- `ThermalMonitorNeuron` — 1 °C cool-down, 2 °C shutdown.
- `PowerBudgetNeuron` — battery state → NORMAL / CONSERVE / EMERGENCY.
- `CalibrationSchedulerNeuron` — drift / error / interval-driven recalib.

## Integration

- Every `StimulationCommandSignal` must pass
  `StimulationSafetyGateNeuron.veto(...)`; non-null return = hard refusal.
- `ReafferenceComparatorNeuron` (from the `embodiment` module) emits
  `AgencyLossSignal` whenever the predicted and observed body states
  diverge beyond `efference-copy-tolerance` (default 0.15).
- Seizure watchdog output is routed to the safety gate via
  `triggerSeizureLockout(...)`; thermal monitor → `triggerThermalLockout`.
- Calibration events are emitted as `CalibrationSignal` for the
  transparency bus and for the oversight UI.

## Configuration

```yaml
bci:
  enabled: false
  tick-rate-hz: 1000
  stimulation:
    max-charge-density-uc-cm2: 0.5
    net-dc-tolerance-uc: 0.001
    max-frequency-hz: 300
    max-pulse-width-us: 300
  thermal:
    cool-down-delta-c: 1.0
    shutdown-delta-c: 2.0
  seizure:
    risk-threshold: 0.8
    lockout-ticks: 60000
  power:
    battery-capacity-mah: 500
    conserve-frac: 0.30
    emergency-frac: 0.10
  embodiment:
    efference-copy-tolerance: 0.15
  calibration:
    min-interval-ticks: 86400000
    max-interval-ticks: 604800000
    drift-trigger: 0.25
    error-trigger: 0.30
```

Defaults to `enabled: false` for strict backward compatibility.

## Tests

`BciModuleTest` covers:

- Signal frequency constants and round-trip copies (NeuralSpike, LFP,
  Stimulation, Charge, Agency, Drift, Thermal, SensoryFeedback).
- Layer 0: threshold detection, spike-sorting unit assignment, LFP band
  extraction, artefact masking.
- Layer 1: firing-rate EWMA, population-vector direction, Kalman
  convergence, LFADS-latent boundedness, phoneme centroid match.
- Layer 2: intent fusion by confidence; user-state classification.
- Layer 3: Hebbian weight update, drift flag, lexicon cosine match.
- Layer 4: joint-limit clip; grip selection by object size.
- Layer 5: Shannon veto, frequency veto, seizure lockout / recovery,
  charge-balance DC buildup, seizure marker classification,
  actuator dispatch counter.
- Layer 7: thermal cool-down / shutdown, power mode transitions,
  calibration trigger / suppression.
- Config defaults and enum cardinality.

## References

- Georgopoulos, A.P. et al. (1986). Neuronal population coding of movement
  direction. *Science* 233, 1416–1419.
- Wu, W. et al. (2006). Bayesian population decoding of motor cortical
  activity using a Kalman filter. *Neural Computation* 18.
- Velliste, M. et al. (2008). Cortical control of a prosthetic arm for
  self-feeding. *Nature* 453, 1098–1101.
- Hochberg, L.R. et al. (2012). Reach and grasp by people with tetraplegia
  using a neurally controlled robotic arm. *Nature* 485, 372–375.
- Pandarinath, C. et al. (2018). Inferring single-trial neural population
  dynamics using sequential auto-encoders. *Nature Methods* 15, 805–815.
- Flesher, S.N. et al. (2021). A brain-computer interface that evokes
  tactile sensations improves robotic arm control. *Science* 372, 831–836.
- Shannon, R.V. (1992). A model of safe levels for electrical stimulation.
  *IEEE Trans Biomed Eng* 39, 424–426.
- Cogan, S.F. (2008). Neural stimulation and recording electrodes. *Annu
  Rev Biomed Eng* 10, 275–309.
- Worrell, G.A. et al. (2008). High-frequency oscillations in human temporal
  lobe. *Brain* 131, 928–937.
- Wolpert, D.M., Miall, R.C., Kawato, M. (1998). Internal models in the
  cerebellum. *Trends Cogn Sci* 2, 338–347.
- Moses, D.A. et al. (2021). Neuroprosthesis for decoding speech in a
  paralyzed person with anarthria. *N Engl J Med* 385, 217–227.
- Willett, F.R. et al. (2023). A high-performance speech neuroprosthesis.
  *Nature* 620, 1031–1036.
- Perge, J.A. et al. (2013). Intra-day signal instabilities affect decoding
  performance in an intracortical neural interface. *J Neural Eng* 10.
- Andersen, R.A., Buneo, C.A. (2002). Intentional maps in posterior parietal
  cortex. *Annu Rev Neurosci* 25, 189–220.
- Murata, A. et al. (2000). Selectivity for the shape, size, and orientation
  of objects for grasping in neurons of monkey parietal area AIP. *J
  Neurophysiol* 83, 2580–2601.
- Orsborn, A.L. et al. (2014). Closed-loop decoder adaptation shapes neural
  plasticity for skillful neuroprosthetic control. *Neuron* 82, 1380–1393.
- Shadmehr, R., Wise, S.P. (2005). *The Computational Neurobiology of
  Reaching and Pointing*. MIT Press.
