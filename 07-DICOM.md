# Bridge 07 — DICOM (medical imaging context bridge)

> **Prerequisite:** `00-FRAMEWORK.md`.

**Priority:** medium-high. **Safety ceiling:** **READ-ONLY**. The bridge does not push images, reports, or annotations back to the PACS. This is a context bridge, not a radiology AI.

## 1. Domain context

[DICOM](https://www.dicomstandard.org/) is the international standard for medical imaging — both the file format (`.dcm`) and the network protocol (DIMSE) used between modalities (CT, MRI, X-ray, ultrasound), workstations, and PACS (Picture Archiving and Communication System).

Jneopallium is **not** competing with radiology AI. It is not classifying tumours or measuring nodule diameters. The DICOM bridge exists for one reason: to convert imaging *findings* — typically already produced by a radiologist or by an upstream AI in a structured DICOM SR (Structured Report) — into a typed `clinical/ImagingFindingSignal` that the clinical reasoning module can integrate with vitals, labs, and history.

The signal already exists in the repo. This bridge is an adapter, not a new domain.

## 2. Maven dependency

```xml
<!-- dcm4che — the dominant Java DICOM toolkit -->
<dependency>
    <groupId>org.dcm4che</groupId>
    <artifactId>dcm4che-core</artifactId>
    <version>5.32.0</version>
</dependency>
<dependency>
    <groupId>org.dcm4che</groupId>
    <artifactId>dcm4che-net</artifactId>
    <version>5.32.0</version>
</dependency>
<!-- For DICOMweb (REST) clients -->
<dependency>
    <groupId>org.dcm4che</groupId>
    <artifactId>dcm4che-tool-wadors</artifactId>
    <version>5.32.0</version>
</dependency>
```

## 3. Why read-only is the right ceiling

* DICOM SR write-back makes the bridge a participant in the radiology audit trail. That changes the regulatory class.
* Most clinical value is in **integrating** imaging findings with non-imaging context (the FHIR side). Annotations belong with the radiologist, not the network.
* If a deployment ever wants to push annotations, it is a separate, independently-certified bridge.

## 4. Architecture

Two ingress modes, configurable per binding:

* **DIMSE C-FIND / C-MOVE / C-GET** — the classic DICOM network protocol over TCP. Used inside hospital networks.
* **DICOMweb (QIDO-RS / WADO-RS)** — REST over HTTPS. Used in modern cloud-native PACS deployments.

```
Modality → PACS    ◀── C-FIND/C-MOVE ──── DicomClientService
or       → DICOMweb ◀──── QIDO/WADO ──────  • dcm4che Association
                                            • DICOMweb HTTP client
                                            • Study/Series cache
                                            • SR parser
                                          ────────────┬───────────
                                                      │
                                            ┌─────────▼──────────┐
                                            │ DicomFindingInput  │
                                            │  → ImagingFinding  │
                                            │    Signal          │
                                            │  (one per          │
                                            │   structured-report│
                                            │   finding)          │
                                            └────────────────────┘

NO write path. NO C-STORE outbound. NO STOW-RS.
```

## 5. Signal mapping

`ImagingFindingSignal` already exists in `clinical/`. The bridge's only job is to extract findings from DICOM SR documents and instantiate the signal.

| DICOM input | Extraction | Jneopallium signal |
|---|---|---|
| DICOM SR (Structured Report) `Modality=SR` | parse content tree, extract codeable observations | one `ImagingFindingSignal` per leaf observation |
| DICOM Image header | study description, body part, modality | bridge metadata; no signal unless an SR references it |
| Annotation Object (`PR` Presentation State) | structured annotation | optional `ImagingFindingSignal` if the deployment wants annotation-driven findings |

The bridge does **not** read pixel data. Image bytes never enter the JVM heap.

## 6. Configuration

```yaml
mode: "DICOMWEB"                     # or "DIMSE"

dimse:
  callingAet: "JNEO-BRIDGE"
  calledAet:  "PACS"
  host: "pacs.hospital.local"
  port: 11112

dicomweb:
  baseUrl: "https://pacs.hospital.local/dicom-web"
  qidoEndpoint: "/studies"
  wadoEndpoint: "/studies/{study}/series/{series}/instances/{instance}/metadata"

security:
  type: "OAuth2BearerToken"
  tokenEndpoint: "..."
  clientId: "..."
  clientSecretEnv: "DICOM_CLIENT_SECRET"

reads:
  - bindingId: "RECENT-RADIOLOGY-SR"
    studyFilter:
      modality: "SR"
      accessionPattern: "RAD-*"
      windowHours: 24
    targetSignal: "IMAGING_FINDING"
    signalTagPrefix: "EHR.RADIOLOGY"

privacy:
  pseudonymise: true
  saltEnv: "DICOM_PSEUDO_SALT"
  redactPatientName: true
  redactInstitution: false

audit:
  localAuditFile: "/var/log/jneopallium/dicom-audit.jsonl"
```

`writes:` block is **rejected at config-load** with a clear message — there is no write surface.

## 7. Package layout

```
worker/src/main/java/com/rakovpublic/jneuropallium/worker/bridge/dicom/
├── DicomBridgeConfig.java
├── DicomBridgeConfigLoader.java
├── DicomStudyBinding.java
├── DicomSrParser.java                  (extracts findings from SR content trees)
├── DicomSignalMapper.java
├── DicomClientService.java             (DIMSE Association OR DICOMweb HTTP)
├── DimseClient.java                    (dcm4che Association wrapper)
├── DicomwebClient.java                 (QIDO + WADO HTTP)
└── DicomFindingInput.java
```

No aggregator class — there is no write path.

## 8. Phase plan

| Phase | Goal |
|-------|------|
| 1 | DICOMweb-only against a public test PACS (Orthanc demo, dicomstandard.org PACS). QIDO query, SR fetch, finding extraction. |
| 2 | DIMSE mode for legacy on-prem PACS. C-FIND + C-MOVE to a local AE. |
| 3 | **Permanently disabled.** Write back is a separate bridge. |

## 9. Bridge-specific scenarios

| # | Scenario | Setup | Expected |
|---|----------|-------|----------|
| **S7** | DICOMweb fetch | Orthanc test instance with one SR study | Bridge produces `ImagingFindingSignal`s, one per leaf SR observation |
| **S8** | DIMSE handshake | Local Orthanc with DIMSE listener | Successful association; query returns expected studies |
| **S9** | Patient name redaction | SR contains `PatientName="DOE^JOHN"` | Emitted signal carries no patient name; pseudonymous study id only |
| **S10** | No write surface | Unit test attempts to construct a `DicomCommandOutputAggregator` (class doesn't exist) | Compile error — verifies the architectural choice |
| **S11** | SR with deeply nested findings | SR has 3-level nested observations | All leaves are emitted; intermediate nodes are not duplicated |

## 10. Risks

| # | Risk | Mitigation |
|---|------|------------|
| R1 | Pixel data accidentally pulled | Bridge configures WADO requests with `accept=application/dicom+json` (metadata only); pixel data path is unreachable. |
| R2 | DIMSE TLS configuration drift | Document AE-title/IP allow-list and TLS cert config; explicit failure on plain TCP in production. |
| R3 | SR coding scheme not supported | Bridge supports SNOMED CT, LOINC, RadLex by default; unknown codes pass through as opaque strings with `Quality.UNCERTAIN`. |
| R4 | Old PACS implementations with quirky DIMSE | Document the C-FIND attribute set used; allow per-deployment overrides. |

## 11. References

* DICOM standard — `https://www.dicomstandard.org/current`.
* dcm4che — `https://github.com/dcm4che/dcm4che`.
* Orthanc test instance — `https://orthanc.uclouvain.be/demo/`.
