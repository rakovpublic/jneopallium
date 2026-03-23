package com.rakovpublic.jneuropallium.ai.signals.slow;

import com.rakovpublic.jneuropallium.ai.model.NeuronSpec;
import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class StructuralPlasticitySignal extends BaseSignal {
    private NeuronSpec newNeuron; // nullable
    private Long deleteNeuronId; // nullable
    private double trigger;
    private String layerId;

    public StructuralPlasticitySignal() { super(); this.loop = 2; this.epoch = 5L; }
    public StructuralPlasticitySignal(NeuronSpec newNeuron, Long deleteNeuronId, double trigger, String layerId) {
        this(); this.newNeuron = newNeuron; this.deleteNeuronId = deleteNeuronId;
        this.trigger = trigger; this.layerId = layerId;
    }

    public NeuronSpec getNewNeuron() { return newNeuron; }
    public void setNewNeuron(NeuronSpec newNeuron) { this.newNeuron = newNeuron; }
    public Long getDeleteNeuronId() { return deleteNeuronId; }
    public void setDeleteNeuronId(Long deleteNeuronId) { this.deleteNeuronId = deleteNeuronId; }
    public double getTrigger() { return trigger; }
    public void setTrigger(double trigger) { this.trigger = trigger; }
    public String getLayerId() { return layerId; }
    public void setLayerId(String layerId) { this.layerId = layerId; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return StructuralPlasticitySignal.class; }
    @Override public String getDescription() { return "StructuralPlasticitySignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        StructuralPlasticitySignal c = new StructuralPlasticitySignal(newNeuron, deleteNeuronId, trigger, layerId);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
