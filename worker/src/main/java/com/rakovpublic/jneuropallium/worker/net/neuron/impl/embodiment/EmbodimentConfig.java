/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.embodiment;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration for the embodiment subsystem.
 * Maps to the {@code embodiment:} top-level section of the jneopallium config.
 * Default {@link #enabled} is {@code false} — embodiment must be opt-in.
 */
public class EmbodimentConfig {

    public static class EffectorSpec {
        private int id;
        private String name;
        private int dof;
        private double healthThreshold;

        public EffectorSpec() {}
        public EffectorSpec(int id, String name, int dof, double healthThreshold) {
            this.id = id;
            this.name = name;
            this.dof = dof;
            this.healthThreshold = healthThreshold;
        }
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getDof() { return dof; }
        public void setDof(int dof) { this.dof = dof; }
        public double getHealthThreshold() { return healthThreshold; }
        public void setHealthThreshold(double healthThreshold) { this.healthThreshold = healthThreshold; }
    }

    private boolean enabled = false;
    private List<EffectorSpec> effectors = new ArrayList<>();
    private double efferenceCopyMismatchThreshold = 0.15;
    private double efferenceCopyFailureEmitThreshold = 0.4;
    private boolean toolIncorporationEnabled = true;
    private int toolIncorporationTimeoutTicks = 600;

    public EmbodimentConfig() {}

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public List<EffectorSpec> getEffectors() { return effectors; }
    public void setEffectors(List<EffectorSpec> effectors) { this.effectors = effectors; }

    public double getEfferenceCopyMismatchThreshold() { return efferenceCopyMismatchThreshold; }
    public void setEfferenceCopyMismatchThreshold(double v) { this.efferenceCopyMismatchThreshold = v; }

    public double getEfferenceCopyFailureEmitThreshold() { return efferenceCopyFailureEmitThreshold; }
    public void setEfferenceCopyFailureEmitThreshold(double v) { this.efferenceCopyFailureEmitThreshold = v; }

    public boolean isToolIncorporationEnabled() { return toolIncorporationEnabled; }
    public void setToolIncorporationEnabled(boolean toolIncorporationEnabled) { this.toolIncorporationEnabled = toolIncorporationEnabled; }

    public int getToolIncorporationTimeoutTicks() { return toolIncorporationTimeoutTicks; }
    public void setToolIncorporationTimeoutTicks(int toolIncorporationTimeoutTicks) { this.toolIncorporationTimeoutTicks = toolIncorporationTimeoutTicks; }
}
