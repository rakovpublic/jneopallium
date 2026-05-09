/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.plc4x;

import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeWriteResult;
import com.rakovpublic.jneuropallium.worker.bridge.common.OverrideRegistry;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Output aggregator for the PLC4X bridge — extends
 * {@link AbstractBridgeOutputAggregator} so the universal §2.2 algorithm
 * (interlocks → overrides → clamp → rate-limit → diff-suppress → audit) is
 * applied verbatim, with PLC4X-specific {@link #issueWrite} as the only
 * protocol-specific hook (01-PLC4X.md §6, 00-FRAMEWORK §2.2).
 */
public final class Plc4xCommandOutputAggregator
        extends AbstractBridgeOutputAggregator<Plc4xFieldBinding> {

    private final Plc4xClientService svc;
    private final Map<String, Plc4xFieldBinding> byTag;
    private final Map<String, BridgeSafetyMode> perTag;

    public Plc4xCommandOutputAggregator(Plc4xClientService svc,
                                        Plc4xConfig config,
                                        AbstractBridgeAuditOutput audit) {
        this(svc, config, audit, new OverrideRegistry());
    }

    public Plc4xCommandOutputAggregator(Plc4xClientService svc,
                                        Plc4xConfig config,
                                        AbstractBridgeAuditOutput audit,
                                        OverrideRegistry overrides) {
        super("plc4x", overrides, audit);
        this.svc = Objects.requireNonNull(svc, "svc");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(audit, "audit");

        Map<String, Plc4xFieldBinding> m = new HashMap<>();
        for (Plc4xConfig.WriteBindingConfig wc : config.writes()) {
            m.put(wc.signalTag(), Plc4xFieldBinding.from(wc));
        }
        this.byTag = Map.copyOf(m);
        this.perTag = Map.copyOf(config.perTagSafetyMode());
    }

    @Override
    protected Plc4xFieldBinding binding(String tag) {
        return byTag.get(tag);
    }

    @Override
    protected BridgeSafetyMode safetyMode(Plc4xFieldBinding binding) {
        BridgeSafetyMode m = perTag.get(binding.bindingId());
        return m != null ? m : BridgeSafetyMode.SHADOW;
    }

    @Override
    protected List<Plc4xFieldBinding> bindingsForInterlock(String interlockId) {
        List<Plc4xFieldBinding> out = new ArrayList<>();
        for (Plc4xFieldBinding b : byTag.values()) {
            if (interlockId.equals(b.loopId())) out.add(b);
        }
        return out;
    }

    @Override
    protected boolean operatorConfirmed(ActuatorCommandSignal command) {
        // PLC4X bridge does not implement an operator-confirmation UI; sites
        // that need ADVISORY-mode writes wire one in via a subclass.
        return false;
    }

    @Override
    protected BridgeWriteResult issueWrite(Plc4xFieldBinding binding, double value) {
        Plc4xResponseCode code = svc.write(
                binding.connectionId(), binding.fieldAddress(), value);
        if (code == Plc4xResponseCode.OK) return BridgeWriteResult.ok();
        return BridgeWriteResult.failed("PLC4X:" + code.name());
    }
}
