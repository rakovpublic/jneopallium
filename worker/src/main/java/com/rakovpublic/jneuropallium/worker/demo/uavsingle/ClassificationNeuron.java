package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClassificationNeuron extends Neuron {
    private TargetClassification classification;
    private Map<String, Double> prototype = new LinkedHashMap<>();

    public ClassificationNeuron() {
        super();
        this.currentNeuronClass = ClassificationNeuron.class;
        this.resultClasses.add(ClassificationScoreSignal.class);
        addSignalProcessor(FeatureVectorSignal.class, new ClassificationProcessor());
    }

    public ClassificationNeuron(TargetClassification classification, Map<String, Double> prototype) {
        this();
        this.classification = classification;
        this.prototype = new LinkedHashMap<>(prototype);
    }

    public TargetClassification getClassification() { return classification; }
    public void setClassification(TargetClassification classification) { this.classification = classification; }
    public Map<String, Double> getPrototype() { return prototype; }
    public void setPrototype(Map<String, Double> prototype) {
        this.prototype = prototype == null ? new LinkedHashMap<>() : new LinkedHashMap<>(prototype);
    }

    public ClassificationScoreSignal score(FeatureVectorSignal vector) {
        double distance = featureDistance(vector.getFeatures(), prototype);
        double score = Math.exp(-4.0 * distance);
        ClassificationScoreSignal signal = new ClassificationScoreSignal();
        signal.setMissionId(vector.getMissionId());
        signal.setUavId(vector.getUavId());
        signal.setTick(vector.getTick());
        signal.setFrameId(vector.getFrameId());
        signal.setClassification(classification);
        signal.setFeatureDistance(distance);
        signal.setScore(TargetPriorityProcessor.clamp(score));
        signal.setSourceNeuronId(getId() == null ? -1L : getId());
        return signal;
    }

    private static double featureDistance(Map<String, Double> actual, Map<String, Double> expected) {
        if (expected.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        int count = 0;
        for (Map.Entry<String, Double> entry : expected.entrySet()) {
            double diff = actual.getOrDefault(entry.getKey(), 0.0) - entry.getValue();
            sum += diff * diff;
            count++;
        }
        return Math.sqrt(sum / Math.max(1, count));
    }
}
