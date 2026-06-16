package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.LinkedHashMap;
import java.util.Map;

public class FeatureVectorSignal extends UavSingleSignal {
    private String frameId;
    private Map<String, Double> features = new LinkedHashMap<>();

    public FeatureVectorSignal() {
        setEventType("CONVOLUTION_FEATURE_VECTOR");
    }

    public String getFrameId() { return frameId; }
    public void setFrameId(String frameId) { this.frameId = frameId; }
    public Map<String, Double> getFeatures() { return features; }
    public void setFeatures(Map<String, Double> features) {
        this.features = features == null ? new LinkedHashMap<>() : new LinkedHashMap<>(features);
    }
}
