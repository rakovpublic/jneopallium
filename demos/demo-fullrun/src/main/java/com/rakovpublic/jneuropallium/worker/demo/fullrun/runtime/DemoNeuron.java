package com.rakovpublic.jneuropallium.worker.demo.fullrun.runtime;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

public class DemoNeuron extends Neuron {
    public String demoId;
    public String layerRole;
    public String neuronLabel;

    public DemoNeuron() {
        super();
        currentNeuronClass = DemoNeuron.class;
        run = -1L;
    }

    public String getDemoId() {
        return demoId;
    }

    public void setDemoId(String demoId) {
        this.demoId = demoId;
    }

    public String getLayerRole() {
        return layerRole;
    }

    public void setLayerRole(String layerRole) {
        this.layerRole = layerRole;
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
