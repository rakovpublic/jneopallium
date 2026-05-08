# Bridge 06 — HL7 FHIR (clinical decision support)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** high. **Safety ceiling:** `ADVISORY` — permanently. This bridge **never** writes a treatment, prescription, or order to a clinical system. Physician confirmation always remains external.

## 1. Domain context

[HL7 FHIR](https://www.hl7.org/fhir/) (Fast Healthcare Interoperability Resources) is the modern standard for healthcare data exchange. It defines a set of REST endpoints over typed JSON/XML resources: `Patient`, `Observation`, `Condition`, `Medication`, `MedicationRequest`, `MedicationAdministration`, `DiagnosticReport`, `AllergyIntolerance`, `Procedure`, `CarePlan`, etc.

Most modern EHRs (Epic, Cerner, Athena, openEHR-derived systems) expose a FHIR API. A bridge into FHIR lets Jneopallium's `clinical/` module reason over real EHR data — observations, lab results, diagnoses, medications — and emit advisory `TreatmentProposalSignal`s back as **annotations**, never as orders.

The repo's `clinical/` package already has every signal type the bridge produces (`VitalSignal`, `LabResultSignal`, `MedicationAdminSignal`, `DemographicSignal`, `DiagnosisHypothesisSignal`, `TreatmentProposalSignal`, `WaveformSignal`, `AdverseEventAlertSignal`, `ClinicalVetoSignal`).

## 2. Maven dependency

```xml
<!-- HAPI FHIR — the reference Java FHIR client/server, R4 + R5 -->
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-base</artifactId>
    <version>7.4.0</version>
</dependency>
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-structures-r4</artifactId>
    <version>7.4.0</version>
</dependency>
<dependency>
    <groupId>ca.uhn.hapi.fhir</groupId>
    <artifactId>hapi-fhir-client</artifactId>
    <version>7.4.0</version>
</dependency>
```

R5 is supported but most live FHIR endpoints in 2026 still expose R4. The bridge supports both via a config flag.

## 3. Why advisory only — structural enforcement

Three rules baked into the bridge:

1. The aggregator's HTTP method whitelist is `GET` only. There is no code path that issues a `POST`, `PUT`, `PATCH`, or `DELETE` against a FHIR resource. Verified by a unit test that mocks `IGenericClient` and asserts only `read()`/`search()` are called.
2. `TreatmentProposalSignal`s emitted by Jneopallium are written to the bridge's local audit JSONL **and** to a configurable advisory file or message queue, but **never** as a FHIR `MedicationRequest` or `ServiceRequest`.
3. A safety filter strips any patient identifier from emitted signals before persistence — the audit trail uses an internal pseudonymous ID. The mapping is held in a separate, restricted-access store outside this bridge.

## 4. Architecture

```
┌──────────────────────┐    GET /Patient/123,        ┌────────────────────────┐
│ FHIR server          │    /Observation?subject=… , │ FhirClientService      │
│  (Epic, Cerner,      │    /Condition?subject=… ,   │  • HAPI IGenericClient │
│   HAPI, public test) │    /MedicationAdmin…        │  • polling scheduler   │
│                      │ ◀────────────────────────── │  • per-resource cache  │
│                      │                             │  • pseudonymisation    │
└──────────────────────┘                             └─────┬───────────┬──────┘
                                                           │           │
                                                ┌──────────▼─┐  ┌──────▼──────┐
                                                │ FhirObs    │  │ FhirMed     │
                                                │ Input      │  │ Input       │
                                                │  → Vital   │  │  → MedAdmin │
                                                │  → LabResult│ │  → Adverse  │
                                                └────────────┘  └─────────────┘
                                                              ▼
                                  [Pipeline → FhirAdvisoryOutputAggregator]
                                                              ▼
                                  WRITE-OUTSIDE-FHIR: local JSONL + advisory queue
                                  No FHIR write of any kind.
```

## 5. Signal mapping

All target signals already exist; this bridge is mapper + REST polling client.

| FHIR resource | Slot | Jneopallium signal |
|---|---|---|
| `Patient` | demographics | `DemographicSignal` (age, sex, anonymised id) |
| `Observation` (vital category) | value, code (LOINC) | `VitalSignal` |
| `Observation` (laboratory category) | value, code, unit, reference range | `LabResultSignal` |
| `Observation` (waveform: ECG, SpO₂) | sampled data | `WaveformSignal` |
| `Condition` | code, clinical status, severity | `DiagnosisHypothesisSignal` (ingested as confirmed prior, not new hypothesis) |
| `MedicationAdministration` | medication, dose, route, time | `MedicationAdminSignal` |
| `AllergyIntolerance` | substance, reaction, severity | `AdverseEventAlertSignal` |
| `DiagnosticReport` | code, conclusion | `DiagnosisHypothesisSignal` |
| `Procedure` | code, status | metadata; not a signal in v1 |

Egress is **outside-FHIR only**:

| Jneopallium signal | Egress channel |
|---|---|
| `TreatmentProposalSignal` | local JSONL audit + advisory file at `audit.advisoryFile` |
| `ClinicalVetoSignal` | same path; recorded with `verdict=VETO` |
| `AdverseEventAlertSignal` | same path |

## 6. Configuration

```yaml
fhir:
  baseUrl: "https://fhir.epic.example.com/api/FHIR/R4"
  fhirVersion: "R4"                  # or R5
  pollIntervalSeconds: 60

security:
  type: "OAuth2BearerToken"          # or "BasicAuth", "MutualTLS"
  tokenEndpoint: "https://auth.epic.example.com/oauth2/token"
  clientId: "jneopallium-bridge"
  clientSecretEnv: "FHIR_CLIENT_SECRET"
  scope: "system/Observation.read system/Patient.read system/Condition.read"

cohort:
  patientIds: ["abc123", "def456"]   # or:
  cohortQuery: "subject:in=Group/diabetes-cohort-2026"

reads:
  - bindingId: "VITAL-HR"
    fhirSearch: "Observation?category=vital-signs&code=8867-4&patient={pid}"
    targetSignal: "VITAL"
    signalTag: "EHR.HR"

  - bindingId: "LAB-GLUCOSE"
    fhirSearch: "Observation?category=laboratory&code=2339-0&patient={pid}"
    targetSignal: "LAB_RESULT"
    signalTag: "EHR.GLUCOSE"

  - bindingId: "MED-ADMIN-INSULIN"
    fhirSearch: "MedicationAdministration?medication.code=insulin&patient={pid}"
    targetSignal: "MED_ADMIN"
    signalTag: "EHR.INSULIN"

privacy:
  pseudonymise: true                 # patientId → SHA256(patientId + bridgeSalt)
  saltEnv: "FHIR_PSEUDO_SALT"
  redactFreeText: true               # strip Observation.note, Condition.note

audit:
  localAuditFile: "/var/log/jneopallium/fhir-audit.jsonl"
  advisoryFile: "/var/log/jneopallium/fhir-treatment-proposals.jsonl"

# perTagSafetyMode is irrelevant — bridge never writes to FHIR.
# AUTONOMOUS is rejected at config-load.
```

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/fhir/
├── FhirBridgeConfig.java
├── FhirBridgeConfigLoader.java
├── FhirSearchBinding.java
├── FhirResourceMapper.java
├── PseudonymService.java               (cohort_id ↔ internal pseudonymous id)
├── FhirClientService.java              (HAPI IGenericClient, scheduled polling)
├── FhirObservationInput.java
├── FhirMedicationInput.java
├── FhirConditionInput.java
└── FhirAdvisoryOutputAggregator.java   (writes outside-FHIR only)
```

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | GET-only against the [HAPI public test server](https://hapi.fhir.org/baseR4) for `Patient`, `Observation`, `Condition`. Pseudonymisation pipeline. |
| 2 | Cohort polling, `MedicationAdministration` and `AllergyIntolerance` mapping. Advisory file emission. |
| 3 | **Permanently disabled.** Production EHR write integration is out of scope. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | HAPI public test fetch | `baseUrl` set to `https://hapi.fhir.org/baseR4`, `cohort.patientIds = ["example-pid-1"]` | Bridge fetches and emits `DemographicSignal` for one patient within `pollIntervalSeconds`. |
| **S8** | OAuth refresh | Token expires mid-run | Bridge transparently refreshes; no signals lost. |
| **S9** | Pseudonymisation | A patient identifier appears in resource JSON | Emitted signal carries SHA256-derived id; original PID never appears in audit JSONL. |
| **S10** | Write attempt blocked | A unit test injects a `TreatmentProposalSignal` and asserts no `IGenericClient.create()`/`update()`/`delete()` is called | Test passes by virtue of those code paths not existing in the bridge. |
| **S11** | Free-text redaction | Observation has `note = "patient mentioned suicidal ideation"` | Mapped signal carries no note; bridge log records that a redaction occurred (count only, no content). |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Patient identifier leakage to logs | Pseudonymisation pipeline mandatory; tested in S9; bridge log format prohibits raw PIDs and lints check this. |
| R2 | Polling load on an EHR | Min `pollIntervalSeconds=15`; document target rate with the EHR's IT department. |
| R3 | FHIR version drift between sites | R4 + R5 supported; per-site config flag. R3-only sites are explicitly out of scope. |
| R4 | Untrusted certificate chain on a self-hosted EHR | Truststore configurable per deployment; never `--insecure`. |
| R5 | HAPI dependency tree size | HAPI pulls a lot of transitive deps; verify no shading conflicts with worker's existing Jackson/log4j stack. |

## 11. Regulatory posture

The bridge is **not** a medical device under FDA SaMD framework or EU MDR Article 2 — it does not make a treatment decision. It produces advisory annotations for clinician review. The bridge's read-only architecture is the structural argument that the same code, deployed elsewhere, could not unintentionally cross into Class II SaMD territory.

If a deployment ever needs to write back to FHIR (e.g. a `Communication` resource with the Jneopallium proposal as a note), that is a **separate** bridge with its own certification path. Don't reuse this bridge for it.

## 12. References

* HL7 FHIR — `https://www.hl7.org/fhir/`.
* HAPI FHIR — `https://hapifhir.io/`.
* HAPI public test server — `https://hapi.fhir.org/`.
* SaMD framework — `https://www.fda.gov/medical-devices/digital-health-center-excellence/software-medical-device-samd`.
