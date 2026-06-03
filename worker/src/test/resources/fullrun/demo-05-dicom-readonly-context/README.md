# Demo 05: DICOM Read-Only Context

Story: a DICOM-context bridge reads study metadata such as modality, body part, series count, study age, and accession completeness.

Network: metadata input, context feature extraction, routing/QC advisory, and result conversion layers.

Safety ceiling: `READ-ONLY`. The demo performs no pixel diagnosis and no writeback. Missing accession or study metadata triggers a quality-control advisory.
