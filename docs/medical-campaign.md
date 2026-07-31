# Medical campaign

## Positioning and evidence boundary

Medical outreach is divided into six distinct subcampaigns. The repository currently demonstrates a
synthetic FHIR-like clinician advisory demo and a DICOM metadata-only read-only context demo. It also
contains clinical domain types and FHIR/DICOM bridge specifications. Those facts support a research
or integration conversation; they do not establish clinical effectiveness, regulatory approval,
diagnostic performance, patient safety, production readiness, or deployment in a healthcare
organization.

Run the nine-demo suite, including the relevant medical demos, from the repository root:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\demo-fullrun\run_all_fullrun_demos.ps1
```

```bash
scripts/demo-fullrun/run_all_fullrun_demos.sh
```

The recorded FHIR demo uses synthetic observations and produces clinician-review advisory cards. Its
default evidence reports 80 ticks/80 output rows and no autonomous orders or writes. The DICOM demo
uses metadata only, produces routing/quality-control advisories, and reports 60 ticks/60 output rows
with no pixel diagnosis or writeback. See the corresponding reports under
`docs/demo-fullrun/reports/` and capability IDs in `campaign/reports/capability-registry.json`.

## 1. Health-IT interoperability

Audience: health-IT integrators, EHR integration vendors, FHIR consultants, clinical decision-
support platform teams, and clinical informatics groups.

Proposition: evaluate mapping FHIR events into typed, multi-timescale Jneopallium signals and return
evidence-linked advisory cards behind a human gate. What exists is a synthetic FHIR-like full-run
demo and an advisory bridge specification. Customer work would include profile/version agreement,
terminology and consent policy, pseudonymization, authentication, validation, observability, and an
approved destination for advisory output. The first step is a synthetic bundle/schema workshop, not
EHR write access.

## 2. Clinical research

Audience: digital-health research groups, clinical informatics laboratories, medical simulation
teams, and research-oriented decision-support vendors.

Proposition: test hypotheses about longitudinal event processing, alert prioritization, patient-
state signal fusion, and evidence trails using synthetic, de-identified, or openly licensed data.
Compare against explicit rules and a conventional baseline, pre-register metrics, preserve negative
results, and keep every output advisory. No treatment recommendation, diagnosis, effectiveness
claim, or prospective patient use is included.

## 3. Hospital operations

Audience: hospital innovation teams and vendors working on resource flow, bed/queue operations,
equipment logistics, or operational simulation.

Proposition: design a synthetic discrete-event proof of concept for resource-flow anomalies and
human-reviewed operational advisories. Reusable pieces are the typed-event runtime, fast/slow
processing, audit records, and observability bridges; no hospital-operations demo currently exists.
The plan needs site-specific workflow assumptions, baseline definition, fairness review, and clear
separation from care prioritization. It must not optimize in a way that denies or delays clinical
care.

## 4. Medical imaging workflow

Audience: imaging workflow vendors, radiology software vendors, PACS/RIS integrators, and DICOM
consultancies.

Proposition: configure the existing read-only DICOM metadata demo for study-context enrichment,
missing-metadata quality checks, and review routing. What exists does not inspect pixels or diagnose
images. A customer POC would agree a metadata allowlist, de-identification, DICOM/DICOMweb profile,
PACS boundary, audit format, and false-positive review. It remains read-only with no PACS/RIS
writeback.

## 5. Medical-device monitoring

Audience: medical-device manufacturers, remote-monitoring research vendors, interoperability teams,
and safety engineering groups.

Proposition: design a synthetic signal-replay POC for longitudinal device-state monitoring,
data-quality flags, and human-reviewed escalation. Existing reusable evidence comes from typed
signals, industrial monitoring, safety gating, FHIR concepts, and audit output, but the repository
does not currently demonstrate a regulated medical-device integration. It requires device protocol,
risk classification, cybersecurity, alarm ownership, validation, and regulatory review. It must not
change therapy or device control.

## 6. Pharmaceutical and bioprocess digital twins

Audience: pharmaceutical manufacturing teams, bioprocess manufacturers, digital-twin researchers,
and systems integrators.

Proposition: evaluate deterministic co-simulation and supervisory advisory logic using FMI/OPC UA-
style integration over a synthetic process model. Existing industrial FMI and safety-gating demos
may be reusable; a pharmaceutical or bioprocess claim is not currently demonstrated. Site-specific
work must establish process model validity, GxP/data-integrity boundaries, change control, qualified
infrastructure, and operator authority. The POC must not release a batch or autonomously change a
validated process.

## Universal medical restrictions

Every medical asset and demo must:

- use synthetic, de-identified, or openly licensed data and exclude real patient data by default;
- remain advisory, read-only, or simulation-only and include a visible human-review gate;
- never diagnose, prescribe, make treatment orders, or substitute for a clinician;
- never claim clinical effectiveness, regulatory approval, or suitability for production use;
- log each recommendation, input provenance, source, limitation, and review decision;
- escalate patient information, clinical claims, regulatory questions, security concerns, and
  deployment promises to qualified humans;
- define data minimization, retention, pseudonymization, access control, incident response, and
  deletion before any authorized non-synthetic dataset is considered.

## Qualification and next step

A qualified prospect has a documented interoperability or research problem, an authorized technical
owner, a synthetic/de-identified evaluation path, and willingness to define a baseline and human
review. Disqualify requests for autonomous diagnosis/treatment, unreviewed patient-data transfer,
pixel diagnostic claims, regulatory shortcuts, or unsafe write access. The normal call to action is
a 30-minute architecture and safety-boundary review followed by a written synthetic POC plan.
