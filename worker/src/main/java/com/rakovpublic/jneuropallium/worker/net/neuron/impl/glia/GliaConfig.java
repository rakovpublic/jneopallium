/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.glia;

/**
 * Configuration for the glial support subsystem (astrocytes, microglia,
 * oligodendrocytes / myelination). Default {@link #enabled} is
 * {@code false} — glial features are opt-in.
 */
public class GliaConfig {

    public static class Astrocytes {
        private boolean perLayer = true;
        private double calciumWaveThreshold = 0.4;
        public boolean isPerLayer() { return perLayer; }
        public void setPerLayer(boolean perLayer) { this.perLayer = perLayer; }
        public double getCalciumWaveThreshold() { return calciumWaveThreshold; }
        public void setCalciumWaveThreshold(double v) { this.calciumWaveThreshold = v; }
    }

    public static class Microglia {
        private boolean pruningEnabled = true;
        private int minInactivityTicks = 2000;
        private int maxPruningsPerEpoch = 10;
        public boolean isPruningEnabled() { return pruningEnabled; }
        public void setPruningEnabled(boolean v) { this.pruningEnabled = v; }
        public int getMinInactivityTicks() { return minInactivityTicks; }
        public void setMinInactivityTicks(int v) { this.minInactivityTicks = v; }
        public int getMaxPruningsPerEpoch() { return maxPruningsPerEpoch; }
        public void setMaxPruningsPerEpoch(int v) { this.maxPruningsPerEpoch = v; }
    }

    public static class Myelination {
        private boolean enabled = true;
        private int baselineDelayTicks = 5;
        private int minDelayTicks = 1;
        private int activityWindow = 500;
        private int delayDecrementPerWindow = 1;
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean v) { this.enabled = v; }
        public int getBaselineDelayTicks() { return baselineDelayTicks; }
        public void setBaselineDelayTicks(int v) { this.baselineDelayTicks = v; }
        public int getMinDelayTicks() { return minDelayTicks; }
        public void setMinDelayTicks(int v) { this.minDelayTicks = v; }
        public int getActivityWindow() { return activityWindow; }
        public void setActivityWindow(int v) { this.activityWindow = v; }
        public int getDelayDecrementPerWindow() { return delayDecrementPerWindow; }
        public void setDelayDecrementPerWindow(int v) { this.delayDecrementPerWindow = v; }
    }

    private boolean enabled = false;
    private Astrocytes astrocytes = new Astrocytes();
    private Microglia microglia = new Microglia();
    private Myelination myelination = new Myelination();

    public GliaConfig() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Astrocytes getAstrocytes() { return astrocytes; }
    public void setAstrocytes(Astrocytes astrocytes) { this.astrocytes = astrocytes; }
    public Microglia getMicroglia() { return microglia; }
    public void setMicroglia(Microglia microglia) { this.microglia = microglia; }
    public Myelination getMyelination() { return myelination; }
    public void setMyelination(Myelination myelination) { this.myelination = myelination; }
}
