package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class HarmModelUpdateSignal extends BaseSignal {
    private String targetNeuronId;
    private double[] weightDeltas;
    private String updateReason;

    public HarmModelUpdateSignal() { super(); this.loop = 2; this.epoch = 1L; }
    public HarmModelUpdateSignal(String targetNeuronId, double[] weightDeltas, String updateReason) {
        this(); this.targetNeuronId = targetNeuronId; this.weightDeltas = weightDeltas; this.updateReason = updateReason;
    }

    public String getTargetNeuronId() { return targetNeuronId; }
    public void setTargetNeuronId(String targetNeuronId) { this.targetNeuronId = targetNeuronId; }
    public double[] getWeightDeltas() { return weightDeltas; }
    public void setWeightDeltas(double[] weightDeltas) { this.weightDeltas = weightDeltas; }
    public String getUpdateReason() { return updateReason; }
    public void setUpdateReason(String updateReason) { this.updateReason = updateReason; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return HarmModelUpdateSignal.class; }
    @Override public String getDescription() { return "HarmModelUpdateSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        HarmModelUpdateSignal c = new HarmModelUpdateSignal(targetNeuronId, weightDeltas, updateReason);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
