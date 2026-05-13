/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
/**
 * DICOM bridge — Bridge 07 (07-DICOM.md).
 *
 * <p>Adapter that converts imaging <i>findings</i> produced by a radiologist
 * or an upstream model — surfaced through DICOM Structured Reports — into
 * {@link com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal}s
 * that the clinical reasoning module can integrate with vitals, labs and
 * history.
 *
 * <p><b>Bridge ceiling: READ-ONLY — permanently.</b> This package contains
 * no aggregator / output / write class. The
 * {@link com.rakovpublic.jneuropallium.worker.bridge.dicom.DicomwebTransport}
 * and {@link com.rakovpublic.jneuropallium.worker.bridge.dicom.DimseClient}
 * seams expose only read primitives — no method takes an HTTP verb or a
 * DICOM message body. The bridge does not read pixel data: WADO requests
 * are pinned to {@code application/dicom+json}, and image instances are
 * never fetched (07-DICOM.md §5, §10 R1 — "Image bytes never enter the
 * JVM heap").
 *
 * <p>Production wiring constructs a
 * {@link com.rakovpublic.jneuropallium.worker.bridge.dicom.JdkHttpDicomwebTransport}
 * (DICOMweb) or an external dcm4che-backed {@link com.rakovpublic.jneuropallium.worker.bridge.dicom.DimseClient}
 * (DIMSE — phase 2 of §8). Acceptance tests use the in-memory
 * implementations.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;
