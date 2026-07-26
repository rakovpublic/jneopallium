package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.enums.HarmVerdict;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class TransparencyLogSignal extends BaseSignal {
    private String actionPlanId;
    private String discriminatorReason;
    private String[] evidenceNeuronIds;
    private HarmVerdict verdict;
    private long timestamp;

    public TransparencyLogSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public TransparencyLogSignal(String actionPlanId, String discriminatorReason, String[] evidenceNeuronIds, HarmVerdict verdict, long timestamp) {
        this(); this.actionPlanId = actionPlanId; this.discriminatorReason = discriminatorReason;
        this.evidenceNeuronIds = evidenceNeuronIds; this.verdict = verdict; this.timestamp = timestamp;
    }

    public String getActionPlanId() { return actionPlanId; }
    public void setActionPlanId(String actionPlanId) { this.actionPlanId = actionPlanId; }
    public String getDiscriminatorReason() { return discriminatorReason; }
    public void setDiscriminatorReason(String discriminatorReason) { this.discriminatorReason = discriminatorReason; }
    public String[] getEvidenceNeuronIds() { return evidenceNeuronIds; }
    public void setEvidenceNeuronIds(String[] evidenceNeuronIds) { this.evidenceNeuronIds = evidenceNeuronIds; }
    public HarmVerdict getVerdict() { return verdict; }
    public void setVerdict(HarmVerdict verdict) { this.verdict = verdict; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return TransparencyLogSignal.class; }
    @Override public String getDescription() { return "TransparencyLogSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        TransparencyLogSignal c = new TransparencyLogSignal(actionPlanId, discriminatorReason, evidenceNeuronIds, verdict, timestamp);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
