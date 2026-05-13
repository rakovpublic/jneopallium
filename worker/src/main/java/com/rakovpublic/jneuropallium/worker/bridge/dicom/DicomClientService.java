/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.dicom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.clinical.ImagingFindingSignal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * DICOM bridge polling client + per-binding cache (07-DICOM.md §4, §7).
 *
 * <p>Owns:
 * <ul>
 *   <li>The {@link DicomwebTransport} <i>or</i> {@link DimseClient} seam —
 *       read-only access to the PACS (§3, §4 diagram). Exactly one of the
 *       two is wired per the bridge mode.</li>
 *   <li>Per-binding queues of decoded {@link ImagingFindingSignal}s
 *       drained by {@link DicomFindingInput} once per tick.</li>
 *   <li>The {@link DicomSrParser} / {@link DicomSignalMapper} pipeline.</li>
 *   <li>Pseudonymisation, applied by the mapper so the queues never carry
 *       a raw {@code PatientID}.</li>
 * </ul>
 *
 * <p>The service has no write methods. There is no aggregator class in
 * this package (§7) and the transport seams expose no write surface — a
 * code path that pushes a DICOM instance back to the PACS cannot be
 * expressed inside the bridge.
 */
public final class DicomClientService implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DicomClientService.class);

    /** Bridge name carried in every audit record (00-FRAMEWORK §4). */
    public static final String BRIDGE_NAME = "dicom";

    /** Default per-binding ring-buffer cap. */
    public static final int DEFAULT_RING_BUFFER = 4096;

    private final DicomBridgeConfig config;
    private final DicomwebClient dicomweb;
    private final DimseClient dimse;
    private final AbstractBridgeAuditOutput audit;
    private final DicomSrParser parser;
    private final DicomSignalMapper mapper;
    private final DicomPseudonymService pseudonyms;

    private final Map<String, DicomStudyBinding> bindings = new LinkedHashMap<>();
    private final Map<String, Deque<IInputSignal>> queueByBinding = new ConcurrentHashMap<>();

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();

    /** DICOMweb wiring (07-DICOM.md §4 — first ingress mode). */
    public static DicomClientService forDicomweb(DicomBridgeConfig config,
                                                 DicomwebTransport transport,
                                                 AbstractBridgeAuditOutput audit) {
        Objects.requireNonNull(transport, "transport");
        if (config.mode() != DicomBridgeConfig.Mode.DICOMWEB) {
            throw new IllegalArgumentException(
                    "DICOMweb transport supplied but config.mode=" + config.mode());
        }
        return new DicomClientService(config, new DicomwebClient(transport, config), null, audit);
    }

    /** DIMSE wiring (07-DICOM.md §4 — second ingress mode). */
    public static DicomClientService forDimse(DicomBridgeConfig config,
                                              DimseClient dimse,
                                              AbstractBridgeAuditOutput audit) {
        Objects.requireNonNull(dimse, "dimse");
        if (config.mode() != DicomBridgeConfig.Mode.DIMSE) {
            throw new IllegalArgumentException(
                    "DIMSE client supplied but config.mode=" + config.mode());
        }
        return new DicomClientService(config, null, dimse, audit);
    }

    DicomClientService(DicomBridgeConfig config,
                       DicomwebClient dicomweb,
                       DimseClient dimse,
                       AbstractBridgeAuditOutput audit) {
        this.config = Objects.requireNonNull(config, "config");
        this.audit = Objects.requireNonNull(audit, "audit");
        this.dicomweb = dicomweb;
        this.dimse = dimse;
        this.pseudonyms = DicomPseudonymService.fromConfig(config.privacy());
        this.parser = new DicomSrParser();
        this.mapper = new DicomSignalMapper(pseudonyms, parser,
                config.privacy().redactPatientName());
    }

    /** Register every read binding. No outlets exist (§3, §7). */
    public synchronized void start() {
        if (started.get() || closed.get()) return;
        for (DicomBridgeConfig.StudyReadConfig r : config.reads()) {
            DicomStudyBinding b = DicomStudyBinding.fromConfig(r);
            bindings.put(b.bindingId(), b);
            queueByBinding.put(b.bindingId(), new ArrayDeque<>());
        }
        started.set(true);
        log.info("DicomClientService started; mode={} bindings={} pseudonymise={}",
                config.mode(), bindings.size(), pseudonyms.isEnabled());
    }

    /**
     * Run one polling cycle: enumerate matching studies for every binding,
     * fetch the SR metadata, parse the content tree, and enqueue one
     * {@link ImagingFindingSignal} per leaf finding.
     */
    public synchronized void poll() {
        if (!isStarted()) return;
        if (!transportReady()) {
            log.debug("DicomClientService.poll: transport not ready, skipping cycle");
            return;
        }
        for (DicomStudyBinding b : bindings.values()) {
            try {
                if (config.mode() == DicomBridgeConfig.Mode.DICOMWEB) {
                    pollDicomweb(b);
                } else {
                    pollDimse(b);
                }
            } catch (IOException | RuntimeException e) {
                audit.append(new BridgeAuditRecord(
                        System.currentTimeMillis(), 0, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.FAILED, b.loopId(), b.signalTagPrefix(),
                        null, null,
                        BridgeAuditRecord.RejectReason.EXCEPTION + ":" + e.getClass().getSimpleName(),
                        null, List.of()));
                log.warn("DicomClientService: binding={} failed: {}", b.bindingId(), e.getMessage());
            }
        }
    }

    private void pollDicomweb(DicomStudyBinding b) throws IOException {
        List<JsonNode> studies = dicomweb.qidoStudies(b);
        int totalFindings = 0;
        for (JsonNode study : studies) {
            List<JsonNode> instances = dicomweb.wadoSrInstancesForStudy(study);
            for (JsonNode srInstance : instances) {
                totalFindings += enqueueFromInstance(b, srInstance);
            }
        }
        log.debug("DicomClientService: binding={} studies={} findings={}",
                b.bindingId(), studies.size(), totalFindings);
    }

    private void pollDimse(DicomStudyBinding b) throws IOException {
        if (dimse == null) return;
        JsonNode q = buildDimseQuery(b);
        List<JsonNode> studies = dimse.cFind(q);
        if (studies.isEmpty()) return;
        List<JsonNode> srInstances = dimse.cMove(q);
        int totalFindings = 0;
        for (JsonNode srInstance : srInstances) {
            totalFindings += enqueueFromInstance(b, srInstance);
        }
        log.debug("DicomClientService: binding={} studies={} findings={} (DIMSE)",
                b.bindingId(), studies.size(), totalFindings);
    }

    private int enqueueFromInstance(DicomStudyBinding b, JsonNode srInstance) {
        DicomSrParser.SrDocument doc = parser.parse(srInstance);
        List<ImagingFindingSignal> signals = mapper.mapDocument(doc);
        if (signals.isEmpty()) return 0;
        for (ImagingFindingSignal s : signals) enqueue(b.bindingId(), s);
        return signals.size();
    }

    private void enqueue(String bindingId, IInputSignal signal) {
        Deque<IInputSignal> q = queueByBinding.get(bindingId);
        if (q == null) return;
        synchronized (q) {
            int dropped = 0;
            while (q.size() >= DEFAULT_RING_BUFFER) {
                q.pollFirst();
                dropped++;
            }
            q.addLast(signal);
            if (dropped > 0) {
                audit.append(new BridgeAuditRecord(
                        System.currentTimeMillis(), 0, BRIDGE_NAME,
                        BridgeAuditRecord.Verdict.REJECTED, bindingId, null, null, null,
                        "RING_BUFFER_OVERFLOW:dropped=" + dropped, null, List.of()));
            }
        }
    }

    private boolean transportReady() {
        if (config.mode() == DicomBridgeConfig.Mode.DICOMWEB) {
            return dicomweb != null && dicomweb.isReady();
        }
        return dimse != null && dimse.isReady();
    }

    private JsonNode buildDimseQuery(DicomStudyBinding b) {
        com.fasterxml.jackson.databind.ObjectMapper m = new com.fasterxml.jackson.databind.ObjectMapper();
        ObjectNode q = m.createObjectNode();
        if (b.modality() != null) q.put("ModalitiesInStudy", b.modality());
        if (b.accessionPattern() != null) q.put("AccessionNumber", b.accessionPattern());
        if (b.windowHours() != null) q.put("WindowHours", b.windowHours());
        return q;
    }

    /** Drain (and clear) the decoded signal queue for one binding. */
    public List<IInputSignal> drain(String bindingId) {
        Deque<IInputSignal> q = queueByBinding.get(bindingId);
        if (q == null || q.isEmpty()) return List.of();
        synchronized (q) {
            List<IInputSignal> snap = new ArrayList<>(q);
            q.clear();
            return snap;
        }
    }

    public boolean isStarted() { return started.get() && !closed.get(); }

    public Map<String, DicomStudyBinding> bindings() { return Map.copyOf(bindings); }

    public DicomBridgeConfig config() { return config; }

    public DicomPseudonymService pseudonymService() { return pseudonyms; }

    public DicomSrParser parser() { return parser; }

    @Override
    public synchronized void close() {
        if (!closed.compareAndSet(false, true)) return;
        try {
            if (dicomweb != null) dicomweb.transport().close();
        } catch (RuntimeException e) {
            log.warn("DicomwebTransport close threw: {}", e.getMessage());
        }
        try {
            if (dimse != null) dimse.close();
        } catch (RuntimeException e) {
            log.warn("DimseClient close threw: {}", e.getMessage());
        }
    }
}
