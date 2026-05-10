/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.canopen;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Test seam for the §10 acceptance scenarios: subclass of
 * {@link AbstractCanopenClientService} that captures every outbound frame
 * in {@link #sentFrames()} and lets tests inject inbound frames via the
 * inherited {@link #onCanFrame(CanFrame)} entrypoint.
 */
final class InMemoryCanopenClientService extends AbstractCanopenClientService {

    private final List<CanFrame> sentFrames = Collections.synchronizedList(new ArrayList<>());
    private boolean failNextSend;

    InMemoryCanopenClientService(CanopenBridgeConfig config, AbstractBridgeAuditOutput audit) {
        super(config, audit);
    }

    @Override
    protected boolean sendRawFrame(CanFrame frame) {
        if (failNextSend) {
            failNextSend = false;
            return false;
        }
        sentFrames.add(frame);
        return true;
    }

    void simulateNextSendFailure() { this.failNextSend = true; }

    List<CanFrame> sentFrames() { return List.copyOf(sentFrames); }

    int sendCount() { return sentFrames.size(); }
}
