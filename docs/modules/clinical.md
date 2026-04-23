# Clinical Decision Support & Differential Diagnosis Module

> Status: implementation of [`use-case-clinical-decision-support.md`](../../use-case-clinical-decision-support.md) for [jneopallium](https://github.com/rakovpublic/jneopallium).
> License: BSD 3-Clause.

---

## Abstract

The clinical module turns jneopallium into a Software-as-a-Medical-Device
(SaMD) decision-support engine for differential diagnosis, treatment
planning, and adverse-event detection. Medical reasoning runs on five
distinct biological timescales — seconds (vitals), minutes–hours (labs),
hours–days (imaging, medication response), days–weeks (treatment
trajectories), years (demographics, chronic disease). The framework's
native `ProcessingFrequency(loop, epoch)` is a direct structural fit:
each clinical signal declares its own cadence rather than being flattened
into one sequence. Crucially, the recommender neuron is advisory only —
executable orders require an explicit physician-confirmation path outside
the network.

## Design Principles

1. **Precautionary principle.** An AI that recommends an action under
   uncertainty about patient harm has already failed. All vetoes flow
   through the existing `HarmVetoSignal` audit path via a
   `ClinicalVetoSignal` specialisation — no new audit route.
2. **Advisory only.** `RecommendationNeuron.MODE` is a final `"advisory"`
   string. `ClinicalConfig#setRecommendationMode` throws on any other
   value, and `confirmation-required` cannot be disabled. These are
   program-level guards against drift into Class III autonomy claims.
3. **Typed timescales.** Vitals at loop 1 / epoch 1, labs at 2/3,
   imaging at 2/5, demographics at 2/10 — the scheduler naturally does
   the right thing without dedicated flattening code.
4. **Patient isolation.** One network instance per patient; no shared
   mutable state across instances. Every signal carries `patientId`.
5. **No between-patient bias vectors.** The `affect`, `curiosity`, and
   `sleep` modules must remain disabled in clinical mode. `ClinicalConfig`
   reports `isAffectDisabled() == isCuriosityDisabled() == isSleepDisabled() == true`.

## Signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical`.

| Class | Loop/Epoch | Notes |
|---|---|---|
| `VitalSignal` | 1/1 | `VitalType`, measurement, timestamp, patientId |
| `WaveformSignal` | 1/1 | ECG/PPG/EEG buffer with `sampleRateHz` |
| `LabResultSignal` | 2/3 | LOINC, value, units, reference range; `isAbnormal()` |
| `ImagingFindingSignal` | 2/5 | modality, BodyPart region, `FindingCategory`, confidence |
| `MedicationAdminSignal` | 2/2 | RxNorm, dose, units, route, timestamp |
| `DemographicSignal` | 2/10 | age, sex, comorbidities, allergies; `isPediatric` / `isGeriatric` |
| `DiagnosisHypothesisSignal` | 1/2 | ICD-10, posterior, supporting evidence ids |
| `TreatmentProposalSignal` | 1/3 | RxNorm or procedure, benefit, risk, rationale (advisory) |
| `AdverseEventAlertSignal` | 1/1 | `AlertSeverity`, event code, detail |
| `ClinicalVetoSignal` | 1/1 | **extends** `HarmVetoSignal`; guideline citation + alternatives |

`ClinicalVetoSignal` extends the existing safety veto deliberately: all
veto decisions — clinical or otherwise — travel the same audit path.
Alternative codes are materialised into `AlternativeAction[]` on the
parent class so downstream transparency logging needs no change.

## Neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical`.

### Layer 0 — perception / monitoring

- **`VitalMonitorNeuron`** (1/1) — rolling per-`VitalType` store plus
  default guardrails (HR 40–150, SpO₂ 88–100, BP_SYS 80–200, BP_DIA
  40–120, TEMP 35.0–39.5, RESP 8–30). Severity of `AdverseEventAlertSignal`
  is derived from the magnitude of excursion (≥0.40→CRITICAL, ≥0.20→URGENT,
  ≥0.05→WARNING, else INFO).
- **`WaveformAnalysisNeuron`** (1/1) — extracts RMS, zero-crossing rate,
  and peak-to-peak from `WaveformSignal` buffers and fires conservative
  pathologic alerts: flat ECG ⇒ `ECG_ASYSTOLE_LIKELY`, high-ZCR ECG ⇒
  `ECG_VF_SUSPECTED`, low-amplitude PPG ⇒ `PPG_LOW_PERFUSION`, flat EEG ⇒
  `EEG_BURST_SUPPRESSION`.
- **`TrendDetectorNeuron`** (1/2) — per-vital rolling window with
  least-squares slope. Emits `UP`, `DOWN`, or `FLAT` against a
  configurable slope threshold.

### Layer 2 — patient context

- **`PatientContextNeuron`** (2/5) — canonical per-patient demographics
  and condition snapshot. Computes a vulnerability factor that
  multiplies harm thresholds: pediatric ×1.5–2.0, geriatric ×1.3–1.75,
  pregnancy ×1.5, immunocompromised ×1.4 (product clamped to 5.0).
- **`AcuityNeuron`** (2/2) — NEWS2-style additive score from the most
  recent vital samples, normalised to `[0,1]`. `harmThresholdMultiplier()`
  returns 1 + 2·acuity — high acuity tightens harm thresholds up to 3×.

### Layer 3 — memory + diagnosis

- **`DifferentialDiagnosisNeuron`** (1/2) — bounded ranked ICD-10
  posterior table. `seed()` inserts candidates at the current uniform
  share so that a fresh-seeded set renormalises to true uniform.
  `update(icd10, likelihoodRatio, evidenceId)` multiplies posteriors and
  re-normalises; `enforceBound()` caps at `maxCandidates`. `ranked()`
  emits `DiagnosisHypothesisSignal` entries above `posteriorThreshold`.
- **`GuidelineMemoryNeuron`** (2/1) — keyed by ICD-10, stores a
  `Guideline` record (recommendation, **citation**, first-line codes,
  guideline-level contraindications, version). Every lookup returns the
  citation string so every downstream recommendation can cite.
- **`DrugInteractionMemoryNeuron`** (2/1) — symmetric RxNorm table of
  `Interaction` records (severity 1–4, mechanism, citation). Maintains
  an active-regimen set; `hazardsFor(proposed)` returns all matching
  interactions. Severity ≥ 4 is treated as a hard contraindication.

### Layer 4 — planning + harm

- **`ClinicalConsequenceModelNeuron`** (1/2) — one-compartment PK model
  (`F·Dose / (Vd·weight)`) with Emax pharmacodynamics and a
  toxic-threshold risk term. Vulnerability from `PatientContextNeuron`
  scales benefit down and risk up.
- **`TreatmentPlanningNeuron`** (1/3) — takes a `Candidate` list, runs
  each through the consequence model, and emits sorted
  `TreatmentProposalSignal`s. Every rationale explicitly contains
  "advisory only; physician confirmation required".
- **`ContraindicationNeuron`** (1/1) — hard filter specialising
  `EthicalPriorityNeuron`. Default rules cover β-lactam anaphylaxis,
  sulfa anaphylaxis, NSAIDs in ESRD (N18.6), high-dose acetaminophen in
  hepatic failure (K72.0), and pregnancy teratogens (warfarin,
  isotretinoin, methotrexate, ACE inhibitors). DDI severity-4 routes
  also trigger vetoes. Returns a `ClinicalVetoSignal` with the
  appropriate `HarmVerdict` (CATASTROPHIC for allergy/pregnancy,
  HARMFUL for comorbidity/DDI). Deployments can register additional
  rules via `addAllergyRule`, `addComorbidityRule`,
  `addPregnancyContraindication`.

### Layer 5 — recommendation

- **`RecommendationNeuron`** (1/1) — specialises `ActionSelectionNeuron`.
  `evaluate()` annotates each candidate with its veto (if any) for the
  audit dashboard; `recommend()` returns only non-vetoed candidates
  ranked by benefit minus risk, capped at `topK`. Mode is hard-coded
  `"advisory"`; no `MotorCommandSignal` with `execute=true` can ever be
  emitted from this path.

## Processors

No new stateless processors were introduced — the signal surface is
small enough that state lives naturally in the listed neurons. This
matches the project-wide rule from `CLAUDE.md` ("stateless processors,
stateful neurons").

## Integration

- `HarmContextNeuron` (existing) gains patient vulnerability via the
  `PatientContextNeuron.getVulnerabilityFactor()` multiplier — pediatric,
  geriatric, pregnant, immunocompromised states produce tighter
  thresholds.
- `HarmVetoNeuron` on veto attaches the specific guideline citation
  carried by `ClinicalVetoSignal` (plus any `alternativeCodes` which
  populate the parent `AlternativeAction[]`).
- `TransparencyLogSignal` gains a clinician-readable rendering via the
  `clinicianDashboardEndpoint` setting on `ClinicalConfig`.
- The affect, curiosity, and sleep modules are intentionally **disabled**
  in clinical mode to avoid between-patient drift — verified by
  `ClinicalConfig.isAffectDisabled() == true`, and similarly for the
  other two.

## Configuration

`ClinicalConfig` mirrors section §5 of the use-case spec:

```yaml
clinical:
  enabled: true
  patient-isolation: true
  vital-guardrails:
    HR:   { min: 40, max: 150 }
    SPO2: { min: 88, max: 100 }
    BP_SYS: { min: 80, max: 200 }
    BP_DIA: { min: 40, max: 120 }
    TEMP: { min: 35.0, max: 39.5 }
    RESP: { min: 8, max: 30 }
  differential:
    max-candidates: 10
    posterior-threshold: 0.1
  contraindication:
    source: "rxnorm+snomed"
    refresh-ticks: 100000
  recommendation:
    mode: advisory        # fixed — setter throws otherwise
    confirmation-required: true  # fixed — setter throws otherwise
  transparency:
    log-every-decision: true
    clinician-dashboard-endpoint: "http://..."
  llm:
    cache-ttl-ms: 3600000 # 1 hour — recent-guideline propagation
```

## Tests

`worker/src/test/java/com/rakovpublic/jneuropallium/worker/net/neuron/impl/clinical/ClinicalModuleTest.java`
covers:

- enum cardinalities (5 enums)
- `ProcessingFrequency` correctness for all 10 signals
- `ClinicalVetoSignal instanceof HarmVetoSignal` + copy round-trip
- vital guardrail alerting + severity grading
- ECG asystole / VF / normal discrimination in `WaveformAnalysisNeuron`
- rising / falling / flat trend detection
- pediatric / geriatric / pregnant vulnerability scaling
- NEWS2-style acuity low-vs-high cases
- Bayesian posterior skew + `maxCandidates` enforcement + threshold filter
- guideline lookup + citation
- DDI hazard detection, severity-4 as contraindication
- PK/PD benefit scaling with dose and toxicity with overdose
- treatment planner always emits advisory rationale
- contraindication vetoes for allergy / comorbidity / pregnancy / DDI-4
- clean pass-through for a safe proposal
- recommender excludes vetoed, sorts by benefit − risk, exposes rejected list
- `ClinicalConfig` rejects non-advisory modes and rejects confirmation=false
- affect/curiosity/sleep remain marked disabled

All tests pass. Full worker suite: 339/339 pass with the clinical
module added (283 pre-existing + 56 new), no regressions.

## Validation (per spec §8)

1. *Silent operation retrofit.* Not executed here — requires access to
   adjudicated historical records.
2. *Contraindication test.* Unit tests confirm β-lactam + anaphylaxis,
   NSAID + ESRD, warfarin + pregnancy, and DDI-4 are vetoed. A
   production rollout must add the full 500-case synthetic battery.
3. *Uncertainty handling.* `DifferentialDiagnosisNeuron.hasConfidentWinner`
   returns false when no candidate leads by the margin — rely on this
   to emit `UNCERTAIN` from the surrounding `HarmGateNeuron`.
4. *Audit trail.* `ClinicalVetoSignal` carries the citation; every
   proposal's rationale records the PK/PD forecast and advisory status.
5. *Bias audit.* Left to the operating institution; the framework does
   not itself introduce demographic bias.

## Deployment topology

- One network instance per patient (`ClinicalConfig.patient-isolation=true`).
- Physician dashboard as primary `IOutputAggregator`; order-entry system
  only after physician confirmation.
- Append-only transparency store for every `TransparencyLogSignal` and
  every `ClinicalVetoSignal`.
- Per-institution LLM endpoint; Ollama preferred for PHI deployments.

## Regulatory posture

FDA 510(k) Class II decision support (advisory only; autonomy claims
require Class III and are out of scope). Aligns with IEC 62304:2006+A1:2015
(software lifecycle) and ISO 14971:2019 (risk management). The
existing transparency logging is near-sufficient for IEC 62304
compliance; extend with guideline version tracking via
`GuidelineMemoryNeuron.Guideline#version`.

## Out of scope

Consistent with §11 of the use-case spec:

- Direct EHR writes — output via HL7 FHIR only, and only after
  physician confirmation.
- Autonomous treatment execution — mode remains `advisory`.
- Cross-patient pattern mining without IRB-approved pipeline.
- Use of `affect`, `curiosity`, or `sleep` modules.

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling:
  The Jneopallium Approach.* IJSR 13(7).
- FDA. (2022). *Clinical Decision Support Software — Guidance for Industry
  and FDA Staff.*
- IEC 62304:2006+A1:2015 — Medical device software lifecycle processes.
- ISO 14971:2019 — Application of risk management to medical devices.
- Sutton, R.T. et al. (2020). An overview of clinical decision support
  systems. *npj Digital Medicine* 3, 17.
- Royal College of Physicians. (2017). *National Early Warning Score
  (NEWS) 2.*
- Shannon, R.V. (1992). A model of safe levels for electrical
  stimulation. *IEEE TBME* 39(4).
