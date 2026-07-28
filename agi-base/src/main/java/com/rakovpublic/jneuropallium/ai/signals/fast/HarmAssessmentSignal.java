package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class HarmAssessmentSignal extends BaseSignal {
    private String actionPlanId;
    private HarmVerdict verdict;
    private double[] harmScores; // length=5
    private String[] triggeringConditions;
    private double confidence;

    public HarmAssessmentSignal() { super(); this.loop = 1; this.epoch = 2L; }
    public HarmAssessmentSignal(String actionPlanId, HarmVerdict verdict, double[] harmScores, String[] triggeringConditions, double confidence) {
        this(); this.actionPlanId = actionPlanId; this.verdict = verdict;
        this.harmScores = harmScores; this.triggeringConditions = triggeringConditions; this.confidence = confidence;
    }

    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }
    public HarmVerdict getVerdict() { return verdict; }
    public void setVerdict(HarmVerdict verdict) { this.verdict = verdict; }
    public double[] getHarmScores() { return harmScores; }
    public void setHarmScores(double[] harmScores) { this.harmScores = harmScores; }
    public String[] getTriggeringConditions() { return triggeringConditions; }
    public void setTriggeringConditions(String[] triggeringConditions) { this.triggeringConditions = triggeringConditions; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HarmAssessmentSignal.class; }
    @Override public String getDescription() { return "HarmAssessmentSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        HarmAssessmentSignal c = new HarmAssessmentSignal(actionPlanId, verdict, harmScores, triggeringConditions, confidence);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
