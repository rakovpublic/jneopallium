package com.rakovpublic.jneuropallium.master.model;

public class CreateNeuronRequest {
    private String neuronClass;
    private String neuronJson;
    private Integer layerId;

    public Integer getLayerId() {
        return layerId;
    }

    public void setLayerId(Integer layerId) {
        this.layerId = layerId;
    }


    public String getNeuronClass() {
        return neuronClass;
    }

    public void setNeuronClass(String neuronClass) {
        this.neuronClass = neuronClass;
    }

    public String getNeuronJson() {
        return neuronJson;
    }

    public void setNeuronJson(String neuronJson) {
        this.neuronJson = neuronJson;
    }
}
