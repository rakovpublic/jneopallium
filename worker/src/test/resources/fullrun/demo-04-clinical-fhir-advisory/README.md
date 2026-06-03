# Demo 04: Clinical FHIR Advisory

Story: a FHIR-like clinical stream provides synthetic vital observations and medication context for advisory triage.

Network: observation input, vitals and trend features, clinical advisory, and result conversion layers.

Safety ceiling: `ADVISORY`. The demo never writes treatment orders; high-risk synthetic patients produce clinician-review advisory cards with audit reasons.
