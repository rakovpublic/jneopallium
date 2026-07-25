/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.affect;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for the affect subsystem.
 * Maps to the {@code affect:} top-level section of the jneopallium config.
 * Default {@link #enabled} is {@code false} — affect must be opt-in.
 */
public class AffectConfig {

    private boolean enabled = false;
    private int valenceDecayTicks = 300;
    private int arousalDecayTicks = 150;
    private double harmThresholdClampMin = 1.0;
    private double harmThresholdClampMax = 5.0;
    private List<String> interoceptionSources = Arrays.asList("energy", "homeostasis", "pain");
    private int interoceptionSamplingEpoch = 2;

    public AffectConfig() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getValenceDecayTicks() { return valenceDecayTicks; }
    public void setValenceDecayTicks(int valenceDecayTicks) { this.valenceDecayTicks = valenceDecayTicks; }

    public int getArousalDecayTicks() { return arousalDecayTicks; }
    public void setArousalDecayTicks(int arousalDecayTicks) { this.arousalDecayTicks = arousalDecayTicks; }

    public double getHarmThresholdClampMin() { return harmThresholdClampMin; }
    public void setHarmThresholdClampMin(double harmThresholdClampMin) { this.harmThresholdClampMin = harmThresholdClampMin; }

    public double getHarmThresholdClampMax() { return harmThresholdClampMax; }
    public void setHarmThresholdClampMax(double harmThresholdClampMax) { this.harmThresholdClampMax = harmThresholdClampMax; }

    public List<String> getInteroceptionSources() { return interoceptionSources; }
    public void setInteroceptionSources(List<String> interoceptionSources) { this.interoceptionSources = interoceptionSources; }

    public int getInteroceptionSamplingEpoch() { return interoceptionSamplingEpoch; }
    public void setInteroceptionSamplingEpoch(int e) { this.interoceptionSamplingEpoch = e; }
}
