package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.Neuron;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ClassificationNeuron extends Neuron implements IFeatureClassificationNeuron {
    private TargetClassification classification;
    private Map<String, Double> prototype = new LinkedHashMap<>();
    private List<Map<String, Double>> exemplars = new ArrayList<>();

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
    public List<Map<String, Double>> getExemplars() { return exemplars; }
    public void setExemplars(List<Map<String, Double>> exemplars) {
        this.exemplars = new ArrayList<>();
        if (exemplars != null) {
            for (Map<String, Double> exemplar : exemplars) {
                this.exemplars.add(exemplar == null ? new LinkedHashMap<>() : new LinkedHashMap<>(exemplar));
            }
        }
    }

    public ClassificationScoreSignal score(FeatureVectorSignal vector) {
        double distance = featureDistance(vector.getFeatures(), prototype);
        for (Map<String, Double> exemplar : exemplars) {
            distance = Math.min(distance, featureDistance(vector.getFeatures(), exemplar));
        }
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

    public void adjustPrototype(Map<String, Double> observed, double learningRate) {
        adaptPrototype(observed, Math.max(0.0, learningRate), false);
    }

    public void repelPrototype(Map<String, Double> observed, double learningRate) {
        adaptPrototype(observed, Math.max(0.0, learningRate), true);
    }

    public void addExemplar(Map<String, Double> observed, int maxExemplars) {
        if (observed == null || observed.isEmpty() || maxExemplars <= 0) {
            return;
        }
        Map<String, Double> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : observed.entrySet()) {
            normalized.put(entry.getKey(), TargetPriorityProcessor.clamp(entry.getValue()));
        }
        exemplars.add(normalized);
        while (exemplars.size() > maxExemplars) {
            exemplars.remove(0);
        }
        setChanged(true);
    }

    private void adaptPrototype(Map<String, Double> observed, double learningRate, boolean repel) {
        if (observed == null || observed.isEmpty() || learningRate == 0.0) {
            return;
        }
        Map<String, Double> updated = new LinkedHashMap<>(prototype);
        for (Map.Entry<String, Double> entry : observed.entrySet()) {
            double current = updated.getOrDefault(entry.getKey(), 0.0);
            double delta = learningRate * (entry.getValue() - current);
            updated.put(entry.getKey(), TargetPriorityProcessor.clamp(repel ? current - delta : current + delta));
        }
        prototype = updated;
        setChanged(true);
    }

    private static double featureDistance(Map<String, Double> actual, Map<String, Double> expected) {
        if (expected.isEmpty()) {
            return 1.0;
        }
        double sum = 0.0;
        double weightSum = 0.0;
        for (Map.Entry<String, Double> entry : expected.entrySet()) {
            double diff = actual.getOrDefault(entry.getKey(), 0.0) - entry.getValue();
            double weight = featureWeight(entry.getKey());
            sum += weight * diff * diff;
            weightSum += weight;
        }
        return Math.sqrt(sum / Math.max(1.0, weightSum));
    }

    private static double featureWeight(String featureName) {
        if (featureName == null) {
            return 1.0;
        }
        if (featureName.startsWith("box.")) {
            return 8.0;
        }
        if (featureName.startsWith("morph.")) {
            return 2.0;
        }
        return 1.0;
    }
}
