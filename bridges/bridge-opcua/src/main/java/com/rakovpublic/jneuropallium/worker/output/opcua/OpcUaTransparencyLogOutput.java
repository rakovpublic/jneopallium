/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.output.opcua;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.MiloOpcUaClientService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import org.eclipse.milo.opcua.stack.core.types.builtin.DataValue;
import org.eclipse.milo.opcua.stack.core.types.builtin.NodeId;
import org.eclipse.milo.opcua.stack.core.types.builtin.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Append-only audit log. One JSONL record per accepted, suppressed,
 * clamped, rate-limited or rejected write. The local file write happens
 * on a single-writer thread to guarantee ordering. The optional OPC UA
 * audit-node mirror is best-effort — local file always wins.
 */
public final class OpcUaTransparencyLogOutput implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OpcUaTransparencyLogOutput.class);

    public enum Verdict { APPLIED, REJECTED, INTERLOCK_TRIP, OVERRIDE_HOLD, FAILED }

    private final OpcUaBridgeConfig.AuditConfig cfg;
    private final MiloOpcUaClientService svc;
    private final NodeId opcUaAuditNode;
    private final Path file;
    private final LinkedBlockingQueue<String> queue = new LinkedBlockingQueue<>(50_000);
    private final Thread writerThread;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean fileHealthy = new AtomicBoolean(true);

    public OpcUaTransparencyLogOutput(OpcUaBridgeConfig.AuditConfig cfg,
                                      MiloOpcUaClientService svc) {
        this.cfg = Objects.requireNonNull(cfg);
        this.svc = svc;
        this.file = Path.of(cfg.localAuditFile());
        this.opcUaAuditNode = cfg.opcUaAuditNodeId() == null
                ? null
                : NodeId.parse(cfg.opcUaAuditNodeId());
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            if (!Files.exists(file)) Files.createFile(file);
        } catch (IOException e) {
            log.error("Audit file unavailable at {}: {}", file, e.getMessage());
            fileHealthy.set(false);
        }
        this.writerThread = new Thread(this::drainLoop, "OpcUaAuditWriter");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    public synchronized void record(Verdict verdict,
                                    String loopId,
                                    String tag,
                                    Double proposed,
                                    Double effective,
                                    String reason,
                                    String safetyMode,
                                    long ts,
                                    long run) {
        if (!cfg.writeRejectedToAudit() && verdict == Verdict.REJECTED) return;
        StringBuilder sb = new StringBuilder(192);
        sb.append('{');
        appendNum(sb, "ts", ts).append(',');
        appendNum(sb, "run", run).append(',');
        appendStr(sb, "verdict", verdict.name()).append(',');
        appendStr(sb, "loopId", loopId).append(',');
        appendStr(sb, "tag", tag).append(',');
        appendOptionalNum(sb, "proposed", proposed).append(',');
        appendOptionalNum(sb, "effective", effective).append(',');
        appendStr(sb, "reason", reason).append(',');
        appendStr(sb, "safetyMode", safetyMode);
        sb.append('}').append('\n');
        String line = sb.toString();
        // Local file write is synchronous so audit ordering matches call order
        // and tests don't need to flush a background thread.
        writeLocal(line);
        // Mirror to OPC UA on the writer thread (best-effort, must never block
        // the local write).
        if (!queue.offer(line)) {
            log.warn("Audit mirror queue full — dropping mirror for {}", tag);
        }
    }

    public void recordRejection(IResult r, String reason, long ts, long run) {
        record(Verdict.REJECTED, "?", String.valueOf(r), null, null, reason, null, ts, run);
    }

    public boolean isHealthy() { return fileHealthy.get(); }

    @Override
    public void close() {
        running.set(false);
        writerThread.interrupt();
        try {
            writerThread.join(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void drainLoop() {
        while (running.get() || !queue.isEmpty()) {
            String line;
            try {
                line = queue.poll(200, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                continue;
            }
            if (line == null) continue;
            // Local write was already done synchronously in record(); only
            // the OPC UA mirror runs here.
            mirrorToOpcUa(line);
        }
    }

    private void writeLocal(String line) {
        if (!fileHealthy.get()) return;
        try {
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    new OpenOption[]{StandardOpenOption.APPEND, StandardOpenOption.CREATE});
        } catch (IOException e) {
            if (fileHealthy.compareAndSet(true, false)) {
                log.error("Audit file write failed at {}: {} — bridge will continue but health is degraded",
                        file, e.getMessage());
            }
        }
    }

    private void mirrorToOpcUa(String line) {
        if (svc == null || opcUaAuditNode == null) return;
        try {
            DataValue dv = DataValue.valueOnly(Variant.of(line.endsWith("\n")
                    ? line.substring(0, line.length() - 1) : line));
            svc.writeValue(opcUaAuditNode, dv);
        } catch (Exception e) {
            log.debug("OPC UA audit mirror write failed: {}", e.getMessage());
        }
    }

    private static StringBuilder appendStr(StringBuilder sb, String k, String v) {
        sb.append('"').append(k).append("\":");
        if (v == null) sb.append("null");
        else { sb.append('"'); escape(sb, v); sb.append('"'); }
        return sb;
    }

    private static StringBuilder appendNum(StringBuilder sb, String k, long v) {
        return sb.append('"').append(k).append("\":").append(v);
    }

    private static StringBuilder appendOptionalNum(StringBuilder sb, String k, Double v) {
        sb.append('"').append(k).append("\":");
        if (v == null || Double.isNaN(v) || Double.isInfinite(v)) sb.append("null");
        else sb.append(v);
        return sb;
    }

    private static void escape(StringBuilder sb, String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\', '"' -> { sb.append('\\').append(c); }
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
    }

    /** Test-only: blocks until the writer queue is drained. */
    public void flush() {
        long deadline = System.currentTimeMillis() + 2000;
        while (!queue.isEmpty() && System.currentTimeMillis() < deadline) {
            try { Thread.sleep(20); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
    }

    /** Test-only: List of audit JSONL lines for inspection. */
    public List<String> readAllLines() {
        if (!fileHealthy.get()) return List.of();
        try { return Files.readAllLines(file, StandardCharsets.UTF_8); }
        catch (IOException e) { return List.of(); }
    }
}
