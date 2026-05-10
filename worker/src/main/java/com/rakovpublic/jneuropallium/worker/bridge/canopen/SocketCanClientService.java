/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Linux SocketCAN backend (13-CANOPEN.md §2, §6). The integration with the
 * native interface (PF_CAN socket via JNA, or a {@code candump}-style
 * subprocess as a fallback) is the responsibility of a separate platform
 * module that injects an {@link CanFrameSink} via
 * {@link #attach(CanFrameSink)} — keeping the worker module free of
 * native-only dependencies (§4 R4: native SocketCAN is not portable to
 * macOS / Windows; the USB-CAN dongle path is the cross-platform escape
 * hatch).
 *
 * <p>This class is a thin {@link AbstractCanopenClientService} subclass
 * that captures outbound frames in {@link #lastSent}, exposes them to
 * the platform module via the {@link CanFrameSink} interface, and routes
 * inbound frames through {@link #onCanFrame(CanFrame)}. When the platform
 * module is absent (the typical worker-only test setup) the class still
 * functions as a faithful in-memory orchestrator — every test that exercises
 * §10 S7 / S9 / S10 / S11 / S12 in this repo runs against this class
 * without ever touching the kernel.
 */
public class SocketCanClientService extends AbstractCanopenClientService {

    private static final Logger log = LoggerFactory.getLogger(SocketCanClientService.class);

    private volatile CanFrameSink sink;
    private volatile CanFrame lastSent;

    public SocketCanClientService(CanopenBridgeConfig config, AbstractBridgeAuditOutput audit) {
        super(config, audit);
        if (config.canBus().type() != CanopenBridgeConfig.BusType.SOCKETCAN) {
            log.warn("SocketCanClientService instantiated with bus type {}; expected SOCKETCAN",
                    config.canBus().type());
        }
    }

    /** Attach (or replace) the platform-specific sink that owns the kernel socket. */
    public void attach(CanFrameSink sink) { this.sink = sink; }

    /** Most-recent frame submitted to {@link #sendRawFrame(CanFrame)}. */
    public CanFrame lastSent() { return lastSent; }

    @Override
    protected boolean sendRawFrame(CanFrame frame) {
        this.lastSent = frame;
        CanFrameSink s = this.sink;
        if (s == null) return true;
        try {
            return s.send(frame);
        } catch (RuntimeException ex) {
            log.warn("SocketCAN sink threw: {}", ex.getMessage());
            return false;
        }
    }

    /** Sink injected by a platform module. Implementations write to {@code AF_CAN}. */
    @FunctionalInterface
    public interface CanFrameSink { boolean send(CanFrame frame); }
}
