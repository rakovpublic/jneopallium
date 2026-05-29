# Demo 05 — ICU deterioration early-warning (HL7 FHIR, ADVISORY)

> Bridge: **HL7 FHIR** ([`07-DICOM`/`06-FHIR` spec family]) ·
> Module: **[clinical](../modules/clinical.md)** ·
> Safety ceiling: **ADVISORY (regulatory ceiling)** ·
> External system: a FHIR R4 server — the public **HAPI FHIR test server**
> (`https://hapi.fhir.org/baseR4`) for a live read; the bridge also ships
> `InMemoryFhirTransport` for offline runs.
>
> Status: the FHIR bridge is **spec + reference implementation** (per the README
> bridge index it is "spec"); this demo uses the JDK-HTTP transport against a
> public test server and the in-memory transport for CI-style runs.

A clinical decision-support demo. It pulls a patient's vital-sign Observations
from a FHIR server, computes an early-warning acuity score with trend awareness,
checks contraindications against active medications/conditions, and emits an
**advisory** recommendation to a clinician. It never writes an order; identifiers
are pseudonymised before they enter the network.

## Scenario

A patient in a monitored bed has Observations posted to a FHIR server: heart
rate, respiratory rate, SpO₂, systolic blood pressure, temperature, and level of
consciousness. The clinical sub-net maintains a NEWS2-style acuity score,
detects deteriorating trends, cross-checks active `MedicationStatement`s and
`Condition`s for contraindications, and — when acuity escalates — emits an
advisory "review patient / consider escalation" recommendation routed to the
clinician's worklist. A licensed human makes every decision.

## What it demonstrates

| Feature | Where |
|---|---|
| FHIR `Observation` search → typed vital signals | `FhirObservationInput`, `FhirResourceMapper`, `FhirSearchBinding` |
| Active meds / problems as context | `FhirMedicationInput`, `FhirConditionInput` |
| Pseudonymisation at the boundary | `PseudonymService` |
| Per-vital monitoring with physiologic ranges | `VitalMonitorNeuron` / `IVitalMonitorNeuron`, `VitalType` |
| Trend / deterioration detection | `TrendDetectorNeuron` / `ITrendDetectorNeuron` |
| Composite acuity (early-warning score) | `AcuityNeuron` / `IAcuityNeuron`, `AlertSeverity` |
| Contraindication check before recommending | `ContraindicationNeuron`, `DrugInteractionMemoryNeuron` |
| Consequence model for clinical actions | `ClinicalConsequenceModelNeuron` |
| Advisory-only recommendation + audit | `RecommendationNeuron`, `FhirAdvisoryOutputAggregator`, `FhirAuditOutput` |
| Regulatory ADVISORY ceiling (no autonomous order) | `FhirBridgeConfigLoader` |

## Architecture / data flow

```
 FHIR R4 server (HAPI test server / hospital FHIR façade)
   Observation?patient=X&code=HR,RR,SpO2,SBP,Temp,LOC
   MedicationStatement?patient=X     Condition?patient=X
        │ HTTPS (JdkHttpFhirTransport)         │
        ▼                                       ▼
  ┌──────────────────────────────────────────────────────┐
  │ FhirClientService  →  PseudonymService (strip PII)     │
  └───┬───────────────────────┬───────────────┬───────────┘
      ▼                       ▼               ▼
 FhirObservationInput   FhirMedicationInput  FhirConditionInput
   → VitalSignal           → context           → context
      │
      ▼
  ┌──────────────────────────────────────────────────────────┐
  │ Clinical sub-net (worker/.../impl/clinical):              │
  │  VitalMonitor(per VitalType) → TrendDetector              │
  │     → Acuity (NEWS2-style composite, AlertSeverity)       │
  │  Contraindication ⟵ DrugInteractionMemory / Guideline     │
  │  ClinicalConsequenceModel (project the recommendation)    │
  │  Recommendation (advisory text + severity)                │
  │  SafetyGate(mode=ADVISORY)                                │
  └───────────────────────────┬──────────────────────────────┘
                              ▼ List<IResult>
  ┌──────────────────────────────────────────────────────────┐
  │ FhirAdvisoryOutputAggregator (advisory only)              │
  │  emits a flagged Observation/Communication in an advisory │
  │  namespace — never a MedicationRequest/ServiceRequest     │
  └───────────────────────────┬──────────────────────────────┘
                              ▼
            Clinician worklist (human-in-the-loop)  +  FhirAuditOutput
```

## Components used

* **Signals**: vital-sign measurements (clinical `VitalSignal` via
  `FhirObservationInput`; slow-loop acuity/alert), `AlarmSignal`. Vital kinds via
  `VitalType`; severities via `AlertSeverity`.
* **Neurons** (`worker.net.neuron.impl.clinical`): `PatientContextNeuron`,
  `VitalMonitorNeuron`, `TrendDetectorNeuron`, `AcuityNeuron`,
  `ContraindicationNeuron`, `DrugInteractionMemoryNeuron`, `GuidelineMemoryNeuron`,
  `ClinicalConsequenceModelNeuron`, `RecommendationNeuron`.
  (`WaveformAnalysisNeuron` / `DifferentialDiagnosisNeuron` /
  `TreatmentPlanningNeuron` extend the demo toward richer support.)
* **Bridge** (`worker.bridge.fhir`): `FhirBridgeConfigLoader`,
  `FhirClientService`, `JdkHttpFhirTransport` / `InMemoryFhirTransport`,
  `FhirResourceMapper`, `FhirSearchBinding`, `FhirObservationInput`,
  `FhirMedicationInput`, `FhirConditionInput`, `FhirAdvisoryOutputAggregator`,
  `FhirAuditOutput`, `PseudonymService`.

## Configuration

`/tmp/demo05-icu.yaml`:

```yaml
connection:
  baseUrl: "https://hapi.fhir.org/baseR4"
  transport: "jdk-http"        # or "in-memory" for offline runs
  requestTimeout: "PT10S"
privacy:
  pseudonymise: true           # PseudonymService runs before any neuron sees data
  saltEnv: "FHIR_PSEUDO_SALT"  # env var; never embed the salt

reads:
  observations:
    patientRef: "Patient/example"
    codes:                     # LOINC
      - { code: "8867-4",  vital: HEART_RATE }
      - { code: "9279-1",  vital: RESP_RATE }
      - { code: "2708-6",  vital: SPO2 }
      - { code: "8480-6",  vital: SBP }
      - { code: "8310-5",  vital: TEMPERATURE }
      - { code: "9269-2",  vital: CONSCIOUSNESS }
    lookbackHours: 24
  medications:
    patientRef: "Patient/example"
  conditions:
    patientRef: "Patient/example"

acuity:
  scheme: "NEWS2"
  escalateAtScore: 5           # composite score → advisory escalation
advisory:
  channel: "Communication"     # advisory artefact kind
  namespace: "jneopallium-advisory"
audit:
  localAuditFile: "/tmp/jneopallium-demo05-audit.jsonl"
perTagSafetyMode:
  ICU.ACUITY.ADVISORY: ADVISORY   # AUTONOMOUS rejected by the loader
tickInterval: "PT5S"             # clinical cadence; acuity on the slow loop
```

## Run procedure

1. **Pick a transport.** For a live read, leave `baseUrl` at the HAPI test server
   and choose a `Patient` that has vital-sign Observations (or POST a handful
   first). For deterministic CI-style runs, set `transport: in-memory` and seed
   `InMemoryFhirTransport` with a vitals timeline.

2. **Build and wire the bridge + clinical sub-net:**

   ```java
   var cfg    = FhirBridgeConfigLoader.load(Path.of("/tmp/demo05-icu.yaml"));
   var pseudo = new PseudonymService(cfg);
   var tx     = new JdkHttpFhirTransport(cfg);          // or InMemoryFhirTransport
   var mapper = new FhirResourceMapper(cfg, pseudo);
   var audit  = new FhirAuditOutput(Path.of(cfg.audit().localAuditFile()));
   var svc    = new FhirClientService(cfg, tx, mapper, audit);

   var obsIn  = new FhirObservationInput("fhir-obs", svc);
   var medIn  = new FhirMedicationInput("fhir-med", svc);
   var condIn = new FhirConditionInput("fhir-cond", svc);
   var agg    = new FhirAdvisoryOutputAggregator(svc, audit);
   // build: patientContext → vitalMonitor → trendDetector → acuity →
   //        contraindication → clinicalConsequenceModel → recommendation →
   //        safetyGate(ADVISORY)
   ```

3. **Establish a baseline.** With stable vitals, confirm `VitalSignal`s arrive,
   `AcuityNeuron` reports a low `AlertSeverity`, and no advisory is emitted.

4. **Drive a deterioration.** Post (or simulate) a worsening trend — rising RR,
   falling SpO₂, falling SBP over successive Observations. `TrendDetectorNeuron`
   flags the slope; the composite acuity climbs; once it crosses
   `escalateAtScore`, `RecommendationNeuron` emits an advisory and the aggregator
   writes a flagged `Communication` in the advisory namespace.

5. **Trigger a contraindication.** Seed a `MedicationStatement` /`Condition` that
   makes a candidate recommendation unsafe; `ContraindicationNeuron` must suppress
   or re-rank it (the consequence model rejects the contraindicated option).

6. **Confirm privacy + ceiling.** Inspect any emitted artefact and the audit log:
   no direct identifiers should appear (pseudonymised). Set
   `perTagSafetyMode: AUTONOMOUS` and reload — the loader must reject it.

## Acceptance

* Stable vitals → low acuity, no advisory.
* A worsening trend raises acuity and emits exactly one advisory when the score
  crosses the threshold (no flapping; trend-aware).
* A contraindicated recommendation is suppressed or down-ranked.
* No `MedicationRequest`/`ServiceRequest` (orderable resource) is ever written —
  only advisory `Communication`/flagged-Observation artefacts.
* Pseudonymisation removes direct identifiers before any neuron processing and
  from the audit trail; `AUTONOMOUS` is rejected.

## Safety / regulatory posture

Clinical decision support that influences care is regulated (e.g. as software as
a medical device); the framework's role here is strictly **advisory with a human
in the loop**, which is why the ceiling is structural ADVISORY and autonomous
ordering is rejected at config load. Pseudonymisation at the boundary supports
data-protection requirements; the hash-chained audit supports traceability.
Out of scope: autonomous prescribing, diagnosis of record, anything that bypasses
clinician judgement. See [`../modules/clinical.md`](../modules/clinical.md) for
the full neuron catalogue and regulatory mapping.
