/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cross-platform USB-CAN backend (13-CANOPEN.md §2, §6, §11 R4). Targets
 * Lawicel-style ASCII over a serial line — CANable, Korlan, PCAN-USB
 * adapters in Lawicel-compatibility mode are the typical dev hardware.
 *
 * <p>As with {@link SocketCanClientService}, the actual serial I/O lives
 * in a platform module that injects a {@link UsbCanFrameSink}. This class
 * provides the orchestration; the platform module is responsible for
 * reading/writing Lawicel ASCII frames over jSerialComm or an equivalent
 * cross-platform serial library.
 */
public class UsbCanClientService extends AbstractCanopenClientService {

    private static final Logger log = LoggerFactory.getLogger(UsbCanClientService.class);

    private volatile UsbCanFrameSink sink;
    private volatile CanFrame lastSent;

    public UsbCanClientService(CanopenBridgeConfig config, AbstractBridgeAuditOutput audit) {
        super(config, audit);
        if (config.canBus().type() != CanopenBridgeConfig.BusType.USB_CAN) {
            log.warn("UsbCanClientService instantiated with bus type {}; expected USB_CAN",
                    config.canBus().type());
        }
    }

    public void attach(UsbCanFrameSink sink) { this.sink = sink; }

    public CanFrame lastSent() { return lastSent; }

    @Override
    protected boolean sendRawFrame(CanFrame frame) {
        this.lastSent = frame;
        UsbCanFrameSink s = this.sink;
        if (s == null) return true;
        try {
            return s.send(frame);
        } catch (RuntimeException ex) {
            log.warn("USB-CAN sink threw: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Sink injected by a platform module. Implementations encode each frame
     * as a Lawicel ASCII line ({@code tIIILDDD...\r}) and push it to the
     * dongle's serial port.
     */
    @FunctionalInterface
    public interface UsbCanFrameSink { boolean send(CanFrame frame); }
}
