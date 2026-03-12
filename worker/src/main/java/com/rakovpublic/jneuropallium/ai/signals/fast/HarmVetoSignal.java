package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.model.AlternativeAction;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class HarmVetoSignal extends BaseSignal {
    private String actionPlanId;
    private String vetoReason;
    private HarmVerdict severity;
    private AlternativeAction[] suggestions;

    public HarmVetoSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public HarmVetoSignal(String actionPlanId, String vetoReason, HarmVerdict severity, AlternativeAction[] suggestions) {
        this(); this.actionPlanId = actionPlanId; this.vetoReason = vetoReason;
        this.severity = severity; this.suggestions = suggestions;
    }

    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }
    public String getVetoReason() { return vetoReason; }
    public void setVetoReason(String vetoReason) { this.vetoReason = vetoReason; }
    public HarmVerdict getSeverity() { return severity; }
    public void setSeverity(HarmVerdict severity) { this.severity = severity; }
    public AlternativeAction[] getSuggestions() { return suggestions; }
    public void setSuggestions(AlternativeAction[] suggestions) { this.suggestions = suggestions; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HarmVetoSignal.class; }
    @Override public String getDescription() { return "HarmVetoSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        HarmVetoSignal c = new HarmVetoSignal(actionPlanId, vetoReason, severity, suggestions);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
