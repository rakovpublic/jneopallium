/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.sleep;

/**
 * Configuration for the sleep / replay / offline-consolidation subsystem.
 * Default {@link #enabled} is {@code false} — sleep must be opt-in.
 */
public class SleepConfig {

    public static class Circadian {
        private int cycleTicks = 10000;
        private double nremFraction = 0.6;
        private double remFraction = 0.15;
        public int getCycleTicks() { return cycleTicks; }
        public void setCycleTicks(int v) { this.cycleTicks = v; }
        public double getNremFraction() { return nremFraction; }
        public void setNremFraction(double v) { this.nremFraction = v; }
        public double getRemFraction() { return remFraction; }
        public void setRemFraction(double v) { this.remFraction = v; }
    }

    public static class Replay {
        private ReplayDirection direction = ReplayDirection.REVERSE;
        private double compressionRatio = 10.0;
        private int topKEpisodes = 20;
        public ReplayDirection getDirection() { return direction; }
        public void setDirection(ReplayDirection d) { this.direction = d; }
        public double getCompressionRatio() { return compressionRatio; }
        public void setCompressionRatio(double v) { this.compressionRatio = v; }
        public int getTopKEpisodes() { return topKEpisodes; }
        public void setTopKEpisodes(int v) { this.topKEpisodes = v; }
    }

    public static class Dreaming {
        private int recombinationCount = 5;
        private double maxNoveltyForPlanning = 0.7;
        public int getRecombinationCount() { return recombinationCount; }
        public void setRecombinationCount(int v) { this.recombinationCount = v; }
        public double getMaxNoveltyForPlanning() { return maxNoveltyForPlanning; }
        public void setMaxNoveltyForPlanning(double v) { this.maxNoveltyForPlanning = v; }
    }

    private boolean enabled = false;
    private Circadian circadian = new Circadian();
    private Replay replay = new Replay();
    private Dreaming dreaming = new Dreaming();
    private double consolidationBoost = 3.0;

    public SleepConfig() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Circadian getCircadian() { return circadian; }
    public void setCircadian(Circadian circadian) { this.circadian = circadian; }
    public Replay getReplay() { return replay; }
    public void setReplay(Replay replay) { this.replay = replay; }
    public Dreaming getDreaming() { return dreaming; }
    public void setDreaming(Dreaming dreaming) { this.dreaming = dreaming; }
    public double getConsolidationBoost() { return consolidationBoost; }
    public void setConsolidationBoost(double v) { this.consolidationBoost = Math.max(1.0, v); }
}
