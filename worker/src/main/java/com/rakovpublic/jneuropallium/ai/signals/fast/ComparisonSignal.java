package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ComparisonSignal extends BaseSignal {
    private double predicted;
    private double actual;
    private String contextId;

    public ComparisonSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public ComparisonSignal(double predicted, double actual, String contextId) {
        this(); this.predicted = predicted; this.actual = actual; this.contextId = contextId;
    }

    public double getPredicted() { return predicted; }
    public void setPredicted(double predicted) { this.predicted = predicted; }
    public double getActual() { return actual; }
    public void setActual(double actual) { this.actual = actual; }
    public String getContextId() { return contextId; }
    public void setContextId(String contextId) { this.contextId = contextId; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return ComparisonSignal.class; }
    @Override public String getDescription() { return "ComparisonSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        ComparisonSignal c = new ComparisonSignal(predicted, actual, contextId);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
