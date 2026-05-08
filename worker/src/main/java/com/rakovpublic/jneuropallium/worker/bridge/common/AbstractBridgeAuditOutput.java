/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

/**
 * Append-only JSONL writer for {@link BridgeAuditRecord}s
 * (00-FRAMEWORK §4, §6).
 *
 * <p>Writes one record per line, atomically per call. Audit failures are
 * isolated per scenario S5 in §5: an {@link IOException} on append is
 * logged and swallowed — the bridge stays functional with a degraded audit
 * trail rather than blocking writes to the field.
 *
 * <p>Subclasses can override {@link #mirror(BridgeAuditRecord)} to also
 * publish each record to a bridge-specific external channel (OPC UA audit
 * node, Kafka topic, OTel span, FHIR Provenance resource, …). The mirror
 * call is independent of the local file write.
 */
public abstract class AbstractBridgeAuditOutput implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(AbstractBridgeAuditOutput.class);

    private final ObjectMapper mapper;
    private final Path file;
    private BufferedWriter writer;
    private boolean degraded;

    protected AbstractBridgeAuditOutput(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        this.mapper = new ObjectMapper().disable(SerializationFeature.INDENT_OUTPUT);
        try {
            if (file.getParent() != null) Files.createDirectories(file.getParent());
            this.writer = Files.newBufferedWriter(
                    file, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Audit output {} could not be opened, running in degraded mode: {}",
                    file, e.getMessage());
            this.writer = null;
            this.degraded = true;
        }
    }

    /** Append one audit record. Thread-safe. Failures are swallowed (S5). */
    public final synchronized void append(BridgeAuditRecord record) {
        Objects.requireNonNull(record, "record");
        try {
            if (writer != null) {
                String line = mapper.writeValueAsString(record);
                writer.write(line);
                writer.newLine();
                writer.flush();
            }
        } catch (IOException e) {
            if (!degraded) {
                log.warn("Audit append failed; bridge continues degraded: {}", e.getMessage());
                degraded = true;
            }
        }
        try {
            mirror(record);
        } catch (RuntimeException e) {
            log.warn("Audit mirror failed: {}", e.getMessage());
        }
    }

    /** {@code true} if a previous append failed; bridge is operating without local audit. */
    public final boolean isDegraded() { return degraded; }

    /**
     * Optional hook to mirror an audit record to a bridge-specific external
     * channel. Default: no-op. Implementations MUST NOT throw checked
     * exceptions; runtime exceptions are logged and isolated.
     */
    protected void mirror(BridgeAuditRecord record) { /* no-op */ }

    @Override
    public final synchronized void close() {
        if (writer != null) {
            try { writer.close(); } catch (IOException e) {
                log.warn("Failed to close audit writer: {}", e.getMessage());
            }
            writer = null;
        }
    }
}
