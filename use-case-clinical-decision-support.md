# Use Case: Clinical Decision Support & Differential Diagnosis

> **Framework:** [jneopallium](https://github.com/rakovpublic/jneopallium) + autonomous-AI architecture + (optional) LLM integration.
> **Domain:** Clinical decision support — differential diagnosis, treatment planning, adverse-event detection.
> **Why jneopallium fits:** Medicine is a textbook multi-timescale inference problem with strong auditability requirements. The framework's fast/slow loops, typed signals, and harm-discriminator module map directly onto clinical workflows.

---

## 1. Problem framing

Clinical reasoning operates simultaneously across five timescales:

| Timescale | Example signals | Decision cadence |
|---|---|---|
| Seconds | heart rate, SpO₂, BP, waveform morphology | continuous |
| Minutes–hours | point-of-care labs, medication response | intermittent |
| Hours–days | full lab panels, imaging results | daily rounds |
| Days–weeks | disease progression, treatment response | appointment |
| Years | demographics, family history, chronic conditions | once / updated rarely |

Conventional ANN approaches (transformers, LSTMs) flatten these into a single sequence. jneopallium can keep them as first-class distinct signal types with correct `ProcessingFrequency(loop, epoch)` assignments, which directly supports auditability and the precautionary principle already baked into the harm-discriminator module.

**Architectural axiom:** a clinical AI that recommends an action under uncertainty about patient harm has already failed. The existing `UNCERTAIN → precautionary rejection` rule from `HarmGateNeuron` is exactly what medicine requires ("first, do no harm").

---

## 2. Mapping to core framework

| Medical concept | jneopallium primitive |
|---|---|
| Vital sign stream | Fast-loop `ISignal` with epoch 1 |
| Lab result | Slow-loop `ISignal` with epoch 3 |
| Imaging finding | Slow-loop `ISignal` with epoch 5 |
| Diagnosis hypothesis | State in `WorkingMemoryNeuron` slot |
| Differential | Competing candidates in `ActionSelectionNeuron` |
| Treatment decision | `MotorCommandSignal` gated by `HarmGateNeuron` |
| Drug interaction check | `ConsequenceModelNeuron` simulation |
| Contraindication | Hard constraint in `EthicalPriorityNeuron` |
| Clinical guideline | Template injected into `LongTermMemoryNeuron` |
| Literature lookup | `LLMKnowledgeNeuron` with `LLMVerificationNeuron` |

---

## 3. Domain-specific signals

Package: `com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical`

| Signal | Loop / Epoch | Payload |
|---|---|---|
| `VitalSignal` | 1 / 1 | `VitalType` enum (HR, SPO2, BP_SYS, BP_DIA, TEMP, RESP), `double value`, `long timestamp`, `String patientId` |
| `WaveformSignal` | 1 / 1 | `WaveformType` (ECG, PPG, EEG), `double[] samples`, `double sampleRateHz` |
| `LabResultSignal` | 2 / 3 | `String analyteCode` (LOINC), `double value`, `String units`, `double[] referenceRange`, `long resultedAt` |
| `ImagingFindingSignal` | 2 / 5 | `String modality`, `String regionCode` (BodyPart), `FindingCategory category`, `double confidence` |
| `MedicationAdminSignal` | 2 / 2 | `String rxNormCode`, `double dose`, `String units`, `String route`, `long administeredAt` |
| `DemographicSignal` | 2 / 10 | `int ageYears`, `Sex sex`, `List<String> comorbidities`, `List<String> allergies` |
| `DiagnosisHypothesisSignal` | 1 / 2 | `String icd10`, `double posteriorProbability`, `List<String> supportingEvidenceIds` |
| `TreatmentProposalSignal` | 1 / 3 | `String rxNormOrProcedureCode`, `double expectedBenefit`, `double expectedRisk`, `String rationale` |
| `AdverseEventAlertSignal` | 1 / 1 | `AlertSeverity severity`, `String eventCode`, `String patientId` |
| `ClinicalVetoSignal` | 1 / 1 | `String vetoReason`, `List<String> alternatives`, `String guidelineCitation` |

### Critical design note

`ClinicalVetoSignal` **must not** be a new signal type from scratch — it must extend `HarmVetoSignal` from the harm-discriminator module. Clinical veto is a specialisation of safety veto. This preserves the existing guarantee that all veto decisions flow through a single audit path.

---

## 4. Domain-specific neurons

Package: `com.rakovpublic.jneuropallium.worker.net.neuron.impl.clinical`

### Layer 0 (perception) — monitoring

| Class | Loop / Epoch | Role |
|---|---|---|
| `VitalMonitorNeuron` | 1 / 1 | Streams `VitalSignal`; emits `AdverseEventAlertSignal` on guardrails (e.g., HR > 150) |
| `WaveformAnalysisNeuron` | 1 / 1 | Runs small real-time models over `WaveformSignal` (e.g., arrhythmia classifier); emits feature `SpikeSignal`s |
| `TrendDetectorNeuron` | 1 / 2 | Per-vital rolling window; emits `SpikeSignal(feature="trend_up"/"trend_down")` |

### Layer 2 (attention) — patient context

| Class | Loop / Epoch | Role |
|---|---|---|
| `PatientContextNeuron` | 2 / 5 | Maintains current `DemographicSignal` + comorbidity list; specialises `HarmContextNeuron` |
| `AcuityNeuron` | 2 / 2 | Computes acuity score from vitals; modulates attention and harm thresholds |

### Layer 3 (memory + diagnosis)

| Class | Loop / Epoch | Role |
|---|---|---|
| `DifferentialDiagnosisNeuron` | 1 / 2 | Maintains ranked list of candidate ICD-10 diagnoses; Bayesian update on each evidence signal |
| `GuidelineMemoryNeuron` | 2 / 1 | Specialised `LongTermMemoryNeuron`; stores clinical practice guidelines as retrievable templates |
| `DrugInteractionMemoryNeuron` | 2 / 1 | Maintains drug-drug interaction table; emits `SpikeSignal(hazard=true)` on match |

### Layer 4 (planning + harm)

| Class | Loop / Epoch | Role |
|---|---|---|
| `TreatmentPlanningNeuron` | 1 / 3 | Specialises `PlanningNeuron`; candidate treatments simulated via `ConsequenceModelNeuron` |
| `ContraindicationNeuron` | 1 / 1 | Hard filter — specialises `EthicalPriorityNeuron`. Anaphylaxis history + β-lactam proposal → immediate veto |
| `ClinicalConsequenceModelNeuron` | 1 / 2 | Specialises `ConsequenceModelNeuron` with pharmacokinetic + pharmacodynamic forward models |

### Layer 5 (action selection + recommendation)

| Class | Loop / Epoch | Role |
|---|---|---|
| `RecommendationNeuron` | 1 / 1 | Specialises `ActionSelectionNeuron`. Only emits `TreatmentProposalSignal` with full rationale; never executes |

**Critical:** in clinical deployment, `execute=true` on `MotorCommandSignal` is **never** emitted for treatment signals. The framework remains advisory. A separate "physician-confirmation" signal path is required before any order is released.

---

## 5. Configuration

```yaml
clinical:
  enabled: true
  patient-isolation: true    # one network instance per patient
  vital-guardrails:
    HR:   {min: 40, max: 150}
    SPO2: {min: 88,  max: 100}
    BP_SYS: {min: 80, max: 200}
  differential:
    max-candidates: 10
    posterior-threshold: 0.1
  contraindication:
    source: "rxnorm+snomed"
    refresh-ticks: 100000
  recommendation:
    mode: advisory    # never autonomous
    confirmation-required: true
  transparency:
    log-every-decision: true
    clinician-dashboard-endpoint: "http://..."
```

---

## 6. Integration with autonomous-AI architecture

- `HarmContextNeuron` gains a patient-vulnerability input — pediatric, geriatric, pregnant, immunocompromised states tighten thresholds by their standard factors.
- `HarmVetoNeuron` on veto must attach the specific guideline citation supporting the veto. This extends the existing `AlternativeAction[]` return to `List<CitedAlternative>` where each carries a guideline reference.
- `TransparencyLogSignal` gains a `clinician-readable` rendering for the audit dashboard.
- The affect module, if enabled, is intentionally **disabled** in clinical mode — the system must not develop mood-based biases across patients.

---

## 7. LLM integration

The `LLMKnowledgeNeuron` + `LLMVerificationNeuron` pair is especially valuable here for looking up rare-disease information, recent literature, drug-interaction subtleties. But with domain-specific tightening:

- `LLMConfidenceSignal.verdict = APPLICABLE` is **not** sufficient to influence a diagnosis directly. LLM responses enter only as `working-memory slot with reduced TTL`, per the existing integration spec, and must be cross-referenced against `GuidelineMemoryNeuron` before reaching `DifferentialDiagnosisNeuron`.
- LLM responses **never** feed `TreatmentPlanningNeuron` directly. They inform the clinician via the dashboard, not the recommender.
- Clinical LLM cache TTL is shortened to 1 hour to ensure recent guideline updates propagate.

---

## 8. Validation criteria

Before clinical deployment:

1. **Silent operation retrofit:** run the network in shadow mode against 10,000+ historical cases; compare recommendations against expert-adjudicated ground truth. Required agreement: ≥95% on high-acuity triage decisions.
2. **Contraindication test:** inject 500 synthetic cases with known contraindications. `ContraindicationNeuron` must veto 100%. This is a hard gate, not a percentage goal.
3. **Uncertainty handling:** inject 100 cases where evidence is genuinely ambiguous. The system must emit `UNCERTAIN` (not a confident wrong answer) in ≥90% of these.
4. **Audit trail:** every recommendation must trace to a sequence of `TransparencyLogSignal` entries sufficient for a clinician to reconstruct the reasoning.
5. **Bias audit:** stratify recommendations by age, sex, race. Significant disparate impact → block deployment, not "fix with a knob".

---

## 9. Deployment topology

- **One network instance per patient.** Patient data never co-mingles across network instances. Cluster-HTTP or cluster-gRPC mode with per-patient routing keys.
- **Physician dashboard** as primary `IOutputAggregator`. Raw `MotorCommandSignal` goes to an order-entry system only after physician confirmation.
- **Audit log** as secondary `IOutputAggregator`. All `TransparencyLogSignal` entries persisted to a tamper-evident store (append-only log + periodic hash chain).
- **LLM endpoint** isolated per-institution; no cross-institution sharing. Local Ollama preferred for PHI-sensitive deployments.

---

## 10. Regulatory posture

This is a Software-as-a-Medical-Device (SaMD) deployment. The framework's advisory-only posture plus the hard advisory/autonomous distinction maps to FDA 510(k) Class II for decision support. Claims of autonomy would push into Class III — not recommended for an initial product. Align documentation with IEC 62304 (software lifecycle) and ISO 14971 (risk management). The harm-discriminator's existing transparency logging is near-sufficient for IEC 62304 compliance; extend with version tracking of guidelines.

---

## 11. Out of scope

- Direct EHR writes. Output via HL7 FHIR APIs only, and only after confirmation.
- Autonomous treatment execution. Mode must remain `advisory`.
- Cross-patient pattern mining without explicit IRB-approved pipeline.
- Use of the affect, curiosity, or sleep modules — these introduce between-patient variability or drift that is inappropriate for clinical deployment.

---

## References

- Rakovskyi, D. (2024). *Framework for Natural Neuron Network Modeling: The Jneopallium Approach.* IJSR 13(7).
- FDA. (2022). *Clinical Decision Support Software — Guidance for Industry and FDA Staff.*
- IEC 62304:2006+A1:2015 — Medical device software lifecycle processes.
- ISO 14971:2019 — Application of risk management to medical devices.
- Sutton, R.T. et al. (2020). An overview of clinical decision support systems. *npj Digital Medicine* 3, 17.
