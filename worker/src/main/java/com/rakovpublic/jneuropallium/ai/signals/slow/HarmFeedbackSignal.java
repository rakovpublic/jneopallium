package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class HarmFeedbackSignal extends BaseSignal {
    private String actionPlanId;
    private boolean actualHarmOccurred;
    private double[] observedHumanStateChange; // length=5
    private String feedbackSource;

    public HarmFeedbackSignal() { super(); this.loop = 2; this.epoch = 1L; }
    public HarmFeedbackSignal(String actionPlanId, boolean actualHarmOccurred, double[] observedHumanStateChange, String feedbackSource) {
        this(); this.actionPlanId = actionPlanId; this.actualHarmOccurred = actualHarmOccurred;
        this.observedHumanStateChange = observedHumanStateChange; this.feedbackSource = feedbackSource;
    }

    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }
    public boolean isActualHarmOccurred() { return actualHarmOccurred; }
    public void setActualHarmOccurred(boolean actualHarmOccurred) { this.actualHarmOccurred = actualHarmOccurred; }
    public double[] getObservedHumanStateChange() { return observedHumanStateChange; }
    public void setObservedHumanStateChange(double[] v) { this.observedHumanStateChange = v; }
    public String getFeedbackSource() { return feedbackSource; }
    public void setFeedbackSource(String feedbackSource) { this.feedbackSource = feedbackSource; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HarmFeedbackSignal.class; }
    @Override public String getDescription() { return "HarmFeedbackSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        HarmFeedbackSignal c = new HarmFeedbackSignal(actionPlanId, actualHarmOccurred, observedHumanStateChange, feedbackSource);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
