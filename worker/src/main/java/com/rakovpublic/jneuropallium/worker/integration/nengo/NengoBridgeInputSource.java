/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.integration.nengo;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeAuditRecord;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.AlarmPriority;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.AlarmSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/**
 * {@link IInitInput} that polls {@link NengoChannelService} once per tick
 * (15-NENGO.md §5).
 *
 * <p>Pipeline per call:
 *
 * <ol>
 *   <li>Drain new {@link NengoInputFrame}s from the channel.</li>
 *   <li>Validate each frame ({@link NengoInputFrame#validate()}).
 *       <ul>
 *         <li>Invalid → audit {@code FAILED reason=FRAME_INVALID:…}, no signal (S10).</li>
 *         <li>Stale (valid_until_ms past now) → audit {@code REJECTED reason=FRAME_STALE}, no signal (S9).</li>
 *       </ul></li>
 *   <li>Emit one {@link NengoDecodedStateSignal} per accepted frame.</li>
 *   <li>Run the {@link NengoInputMapper} to fan out into typed signals.</li>
 *   <li>If the channel reconnected since the last tick, emit one advisory
 *       {@link AlarmSignal} {@code BRIDGE_RECONNECTED} (S8).</li>
 * </ol>
 */
public final class NengoBridgeInputSource implements IInitInput {

    private static final Logger log = LoggerFactory.getLogger(NengoBridgeInputSource.class);

    public static final String BRIDGE_NAME = "nengo";
    public static final ProcessingFrequency PROCESSING_FREQUENCY = new ProcessingFrequency(1L, 1);

    private final String name;
    private final NengoChannelService channel;
    private final NengoInputMapper mapper;
    private final AbstractBridgeAuditOutput audit;

    private long lastReconnectsObserved;
    private long tickRun;

    public NengoBridgeInputSource(String name,
                                  NengoChannelService channel,
                                  NengoInputMapper mapper,
                                  AbstractBridgeAuditOutput audit) {
        this.name = Objects.requireNonNull(name, "name");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.audit = Objects.requireNonNull(audit, "audit");
    }

    @Override
    public List<IInputSignal> readSignals() {
        long now = System.currentTimeMillis();
        long run = ++tickRun;
        List<IInputSignal> out = new ArrayList<>();

        List<NengoInputFrame> frames;
        try {
            frames = channel.pollFrames();
        } catch (RuntimeException ex) {
            audit.append(new BridgeAuditRecord(
                    now, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                    null, null, null, null,
                    BridgeAuditRecord.RejectReason.EXCEPTION + ":" + ex.getClass().getSimpleName(),
                    BridgeSafetyMode.SHADOW, List.of()));
            log.warn("Nengo input poll threw: {}", ex.getMessage());
            return out;
        }

        long reconnects = channel.totalReconnects();
        if (reconnects > lastReconnectsObserved) {
            lastReconnectsObserved = reconnects;
            AlarmSignal a = new AlarmSignal(
                    AlarmPriority.JOURNAL,
                    "NENGO.BRIDGE",
                    "BRIDGE_RECONNECTED",
                    now);
            a.setInputName(name);
            a.setFromExternalNet(true);
            out.add(a);
            audit.append(new BridgeAuditRecord(
                    now, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.APPLIED,
                    null, "NENGO.BRIDGE", null, null,
                    "BRIDGE_RECONNECTED",
                    BridgeSafetyMode.SHADOW, List.of()));
        }

        for (NengoInputFrame f : frames) {
            String invalid = f.validate();
            if (invalid != null) {
                audit.append(new BridgeAuditRecord(
                        now, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.FAILED,
                        null, f.frameId(), null, null,
                        "FRAME_INVALID:" + invalid,
                        BridgeSafetyMode.SHADOW, List.of()));
                continue;
            }
            if (f.validUntilMs() < now) {
                audit.append(new BridgeAuditRecord(
                        now, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.REJECTED,
                        null, f.frameId(), null, null,
                        "FRAME_STALE",
                        BridgeSafetyMode.SHADOW, List.of()));
                continue;
            }

            NengoDecodedStateSignal decoded = NengoDecodedStateSignal.fromFrame(f);
            decoded.setInputName(name);
            decoded.setFromExternalNet(true);
            out.add(decoded);
            out.addAll(mapper.fanOut(decoded, name));

            audit.append(new BridgeAuditRecord(
                    now, run, BRIDGE_NAME, BridgeAuditRecord.Verdict.APPLIED,
                    null, f.frameId(), null, null,
                    "FRAME_ACCEPTED:safety=" + f.safetyStatus(),
                    BridgeSafetyMode.SHADOW, List.of()));
        }
        return out;
    }

    @Override public String getName() { return name; }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResults() { return new HashMap<>(); }

    @Override
    public ProcessingFrequency getDefaultProcessingFrequency() { return PROCESSING_FREQUENCY; }
}
