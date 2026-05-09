/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.bridge.fmi;

import com.rakovpublic.jneuropallium.worker.application.IOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeAuditOutput;
import com.rakovpublic.jneuropallium.worker.bridge.common.AbstractBridgeOutputAggregator;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeSafetyMode;
import com.rakovpublic.jneuropallium.worker.bridge.common.BridgeWriteResult;
import com.rakovpublic.jneuropallium.worker.bridge.common.OverrideRegistry;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Aggregator that enforces the 00-FRAMEWORK §2.2 write algorithm for FMU
 * inputs, then advances the simulation by one step (03-FMI-FMU.md §4).
 *
 * <h2>doStep timing</h2>
 * The FMI doStep call is issued at the end of every tick — after all writes
 * have been applied to the FMU — regardless of whether the tick carried any
 * results. This ensures the simulation clock advances in lock-step with the
 * Jneopallium tick for both AS_FAST_AS_POSSIBLE and REAL_TIME modes.
 *
 * <h2>Implementation note</h2>
 * Because {@link AbstractBridgeOutputAggregator#save} is {@code final}, this
 * class wraps an inner anonymous subclass of that base (Decorator pattern).
 * The inner aggregator handles all safety/audit logic; the outer adds doStep.
 */
public final class FmuCommandOutputAggregator implements IOutputAggregator, AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(FmuCommandOutputAggregator.class);

    private final FmuClientService svc;
    private final InnerAggregator inner;
    private final OverrideRegistry overrideRegistry;

    public FmuCommandOutputAggregator(FmuClientService svc,
                                      FmiBridgeConfig config,
                                      AbstractBridgeAuditOutput audit) {
        this.svc = Objects.requireNonNull(svc, "svc");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(audit, "audit");

        Map<String, FmuVariableBinding> byTag = new HashMap<>();
        for (FmiBridgeConfig.WriteBindingConfig wc : config.writes()) {
            FmuVariableBinding b = new FmuVariableBinding(
                    wc.bindingId(), wc.fmuVariable(), wc.signalTag(),
                    -1,  // valueReference resolved lazily in issueWrite via FmuClientService
                    wc.failSafeValue(), wc.rampRateMaxPerSec(),
                    wc.minClampValue(), wc.maxClampValue());
            byTag.put(wc.signalTag(), b);
        }

        this.overrideRegistry = new OverrideRegistry();
        this.inner = new InnerAggregator(svc, byTag, config.perTagSafetyMode(),
                overrideRegistry, audit);
    }

    /**
     * Apply safety rules to results, write to FMU inputs via
     * {@link FmuClientService#setReal}, then advance the simulation by one
     * step. The doStep call happens even when results is empty.
     */
    @Override
    public void save(List<IResult> results, long timestampMs, long run, IContext context) {
        inner.save(results, timestampMs, run, context);
        try {
            svc.step(timestampMs);
        } catch (FmuException e) {
            log.error("FMU doStep failed at simTime={}: {} [status={}]",
                    svc.simulationTime(), e.getMessage(), e.status());
        }
    }

    /** Read access to the operator override registry (for tests / operator UI). */
    public OverrideRegistry overrideRegistry() { return overrideRegistry; }

    @Override
    public void close() {
        // FmuClientService lifecycle is managed by the caller
    }

    // ===== inner aggregator =================================================

    private static final class InnerAggregator
            extends AbstractBridgeOutputAggregator<FmuVariableBinding> {

        private final FmuClientService svc;
        private final Map<String, FmuVariableBinding> byTag;
        private final Map<String, BridgeSafetyMode> perTag;

        InnerAggregator(FmuClientService svc,
                        Map<String, FmuVariableBinding> byTag,
                        Map<String, BridgeSafetyMode> perTag,
                        OverrideRegistry overrides,
                        AbstractBridgeAuditOutput audit) {
            super("fmi", overrides, audit);
            this.svc = svc;
            this.byTag = Map.copyOf(byTag);
            this.perTag = Map.copyOf(perTag);
        }

        @Override
        protected FmuVariableBinding binding(String tag) {
            return byTag.get(tag);
        }

        @Override
        protected BridgeSafetyMode safetyMode(FmuVariableBinding binding) {
            BridgeSafetyMode m = perTag.get(binding.bindingId());
            return m != null ? m : BridgeSafetyMode.SHADOW;
        }

        @Override
        protected List<FmuVariableBinding> bindingsForInterlock(String interlockId) {
            List<FmuVariableBinding> out = new ArrayList<>();
            for (FmuVariableBinding b : byTag.values()) {
                if (interlockId.equals(b.loopId())) out.add(b);
            }
            return out;
        }

        @Override
        protected boolean operatorConfirmed(ActuatorCommandSignal command) {
            return false; // FMI bridge does not implement operator confirmation UI
        }

        @Override
        protected BridgeWriteResult issueWrite(FmuVariableBinding binding, double value) {
            try {
                svc.setReal(binding.fmuVariable(), value);
                return BridgeWriteResult.ok();
            } catch (FmuException e) {
                return BridgeWriteResult.failed("FMU_EXCEPTION:" + e.getMessage());
            } catch (RuntimeException e) {
                return BridgeWriteResult.failed("EXCEPTION:" + e.getClass().getSimpleName());
            }
        }
    }
}
