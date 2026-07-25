package com.rakovpublic.jneuropallium.ai.neurons.features;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;

/**
 * Feature detector neuron used by TemplateMatchProcessor.
 * Holds a weighted template (receptive-field weight vector) and a detection threshold.
 */
public class FeatureDetectorNeuron extends ModulatableNeuron implements IFeatureDetectorNeuron {

    private double[] weightedTemplate;
    private double detectionThreshold;

    public FeatureDetectorNeuron() {
        super();
        this.weightedTemplate = new double[]{1.0};
        this.detectionThreshold = 0.5;
    }

    public FeatureDetectorNeuron(Long neuronId,
                                 com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain chain,
                                 Long run,
                                 double[] weightedTemplate,
                                 double detectionThreshold) {
        super(neuronId, chain, run);
        this.weightedTemplate = weightedTemplate;
        this.detectionThreshold = detectionThreshold;
    }

    public double[] getWeightedTemplate() { return weightedTemplate; }
    public void setWeightedTemplate(double[] weightedTemplate) { this.weightedTemplate = weightedTemplate; }

    public double getThreshold() { return detectionThreshold; }
    public void setThreshold(double detectionThreshold) { this.detectionThreshold = detectionThreshold; }
}
