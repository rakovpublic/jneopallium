package com.rakovpublic.jneuropallium.worker.demo.autonomousai.runtime;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class AutonomousAiNeuron extends Neuron {
    public String layerRole;
    public String layerName;
    public String neuronLabel;

    public AutonomousAiNeuron() {
        super();
        currentNeuronClass = AutonomousAiNeuron.class;
        run = -1L;
    }

    public String getLayerRole() {
        return layerRole;
    }

    public void setLayerRole(String layerRole) {
        this.layerRole = layerRole;
    }

    public String getLayerName() {
        return layerName;
    }

    public void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    public String getNeuronLabel() {
        return neuronLabel;
    }

    public void setNeuronLabel(String neuronLabel) {
        this.neuronLabel = neuronLabel;
    }

    @Override
    public void setRun(Long run) {
        if (this.run == null || !this.run.equals(run)) {
            this.result.clear();
            this.isProcessed = false;
        }
        super.setRun(run);
    }
}
