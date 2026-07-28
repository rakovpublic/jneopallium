package com.rakovpublic.jneuropallium.ai.model;

public class HarmThreshold {
    private double harmThreshold;
    private double catastrophicThreshold;
    private double uncertainThreshold;

    public HarmThreshold() { this.harmThreshold = 0.3; this.catastrophicThreshold = 0.05; this.uncertainThreshold = 0.5; }
    public HarmThreshold(double harmThreshold, double catastrophicThreshold, double uncertainThreshold) {
        this.harmThreshold = harmThreshold; this.catastrophicThreshold = catastrophicThreshold; this.uncertainThreshold = uncertainThreshold;
    }

    public double getHarmThreshold() { return harmThreshold; }
    public void setHarmThreshold(double harmThreshold) { this.harmThreshold = harmThreshold; }
    public double getCatastrophicThreshold() { return catastrophicThreshold; }
    public void setCatastrophicThreshold(double catastrophicThreshold) { this.catastrophicThreshold = catastrophicThreshold; }
    public double getUncertainThreshold() { return uncertainThreshold; }
    public void setUncertainThreshold(double uncertainThreshold) { this.uncertainThreshold = uncertainThreshold; }
}
