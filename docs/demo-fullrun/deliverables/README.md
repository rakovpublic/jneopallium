# Demo 06 — Cybersecurity Deliverables

Sales- and stakeholder-facing documents for the Jneopallium cybersecurity demo
(Demo 06: Temporal Cybersecurity Threat Correlation). Each deliverable is
provided in **English and Ukrainian**, as both **DOCX and PDF**.

| Deliverable | Audience | English | Ukrainian |
|---|---|---|---|
| **Architecture Article** — complete architecture, written for specialists *and* newcomers | Executives, security leads, engineers | `…-Architecture-Article-EN.{docx,pdf}` | `…-Architecture-Article-UK.{docx,pdf}` |
| **Setup & Deployment Guide** — end-to-end, clone → build → run → train → production advisory | Engineers, DevOps, SRE | `…-Setup-and-Deployment-Guide-EN.{docx,pdf}` | `…-Setup-and-Deployment-Guide-UK.{docx,pdf}` |
| **Test Report** — full verification results with plain-language explanations and honest limitations | QA, security leads, evaluators | `…-Test-Report-EN.{docx,pdf}` | `…-Test-Report-UK.{docx,pdf}` |
| **Pitch Deck** — slide-style sales deck | Buyers, investors, decision-makers | `…-Pitch-Deck-EN.{docx,pdf}` | `…-Pitch-Deck-UK.{docx,pdf}` |

All documents describe an **ADVISORY** (recommend-only) system. Reported model
metrics are pipeline evidence on a deterministic reference corpus, not a
real-world accuracy claim — every document states this explicitly.

## Sources

Content is grounded in the repository's own material:

- `docs/demo-fullrun/reports/demo-06-cybersecurity-kafka-triage-report.md`
- `docs/demo-fullrun/cybersecurity-training-design.md`
- `docs/demo-fullrun/cybersecurity-production-deployment-manual.md`
- `docs/demo-fullrun/reports/demo-06-cybersecurity-production-training-evidence.md`
- `docs/modules/security.md`
- `worker/src/main/resources/model/cybersecurity-temporal/*.json`
- `scripts/demo-cybersecurity-training/train_temporal_model.py`

## Regenerating

The documents are generated from source in `scripts/demo-cybersecurity-training/docgen/`.

```powershell
# 1. Build the DOCX files (requires python-docx)
python scripts/demo-cybersecurity-training/docgen/build_all.py

# 2. Convert to PDF (requires LibreOffice)
$soffice = "C:\Program Files\LibreOffice\program\soffice.exe"
$dir = "docs/demo-fullrun/deliverables"
Get-ChildItem $dir -Filter *.docx | ForEach-Object {
    & $soffice --headless --convert-to pdf --outdir $dir $_.FullName
}
```

To edit content, change the per-document modules (`content_architecture.py`,
`content_deployment.py`, `content_testreport.py`, `content_pitch.py`) and
rebuild. Styling lives in `docbuilder.py`; the block renderer is `blocks.py`.
