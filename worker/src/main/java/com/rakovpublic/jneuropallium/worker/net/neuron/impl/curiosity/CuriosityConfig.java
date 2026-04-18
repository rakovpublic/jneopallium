/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

/**
 * Configuration for the curiosity / intrinsic-motivation subsystem.
 * Maps to the {@code curiosity:} top-level section of the jneopallium
 * config. Default {@link #enabled} is {@code false} — the module must be
 * opt-in.
 */
public class CuriosityConfig {

    public static class Novelty {
        private int hashBits = 2048;
        private int decayTicks = 1000;
        public int getHashBits() { return hashBits; }
        public void setHashBits(int hashBits) { this.hashBits = hashBits; }
        public int getDecayTicks() { return decayTicks; }
        public void setDecayTicks(int decayTicks) { this.decayTicks = decayTicks; }
    }

    public static class LearningProgress {
        private int windowTicks = 200;
        public int getWindowTicks() { return windowTicks; }
        public void setWindowTicks(int windowTicks) { this.windowTicks = windowTicks; }
    }

    public static class Empowerment {
        private int horizon = 3;
        private int nActionSamples = 8;
        public int getHorizon() { return horizon; }
        public void setHorizon(int horizon) { this.horizon = horizon; }
        public int getNActionSamples() { return nActionSamples; }
        public void setNActionSamples(int nActionSamples) { this.nActionSamples = nActionSamples; }
    }

    public static class Weights {
        private double betaNovelty = 0.2;
        private double betaEmpowerment = 0.1;
        public double getBetaNovelty() { return betaNovelty; }
        public void setBetaNovelty(double betaNovelty) { this.betaNovelty = betaNovelty; }
        public double getBetaEmpowerment() { return betaEmpowerment; }
        public void setBetaEmpowerment(double betaEmpowerment) { this.betaEmpowerment = betaEmpowerment; }
    }

    private boolean enabled = false;
    private Novelty novelty = new Novelty();
    private LearningProgress learningProgress = new LearningProgress();
    private Empowerment empowerment = new Empowerment();
    private Weights weights = new Weights();

    public CuriosityConfig() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Novelty getNovelty() { return novelty; }
    public void setNovelty(Novelty novelty) { this.novelty = novelty; }

    public LearningProgress getLearningProgress() { return learningProgress; }
    public void setLearningProgress(LearningProgress learningProgress) { this.learningProgress = learningProgress; }

    public Empowerment getEmpowerment() { return empowerment; }
    public void setEmpowerment(Empowerment empowerment) { this.empowerment = empowerment; }

    public Weights getWeights() { return weights; }
    public void setWeights(Weights weights) { this.weights = weights; }
}
