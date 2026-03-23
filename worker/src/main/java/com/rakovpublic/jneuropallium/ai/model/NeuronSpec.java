package com.rakovpublic.jneuropallium.ai.model;

public class NeuronSpec {
    private String neuronType;
    private double[] initialWeights;
    private String layerId;

    public NeuronSpec() {}
    public NeuronSpec(String neuronType, double[] initialWeights, String layerId) {
        this.neuronType = neuronType; this.initialWeights = initialWeights; this.layerId = layerId;
    }

    public String getNeuronType() { return neuronType; }
    public void setNeuronType(String neuronType) { this.neuronType = neuronType; }
    public double[] getInitialWeights() { return initialWeights; }
    public void setInitialWeights(double[] initialWeights) { this.initialWeights = initialWeights; }
    public String getLayerId() { return layerId; }
    public void setLayerId(String layerId) { this.layerId = layerId; }
}
