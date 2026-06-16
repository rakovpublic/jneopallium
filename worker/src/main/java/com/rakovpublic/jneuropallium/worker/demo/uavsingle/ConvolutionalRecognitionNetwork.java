package com.rakovpublic.jneuropallium.worker.demo.uavsingle;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ConvolutionalRecognitionNetwork {
    private RecognitionNetworkConfig config;
    private List<ConvolutionalPerceptronNeuron> firstLayerNeurons;
    private List<ConvolutionalPerceptronNeuron> secondLayerNeurons;
    private Map<TargetClassification, ClassificationNeuron> classifierNeurons = new EnumMap<>(TargetClassification.class);
    private final Map<String, FeatureVectorSignal> recentFeatureVectors = new LinkedHashMap<>();
    private int learningUpdates;

    public ConvolutionalRecognitionNetwork() {
        this(RecognitionNetworkConfig.fpv1080p());
    }

    public ConvolutionalRecognitionNetwork(RecognitionNetworkConfig config) {
        this.config = config == null ? RecognitionNetworkConfig.fpv1080p() : config;
        this.firstLayerNeurons = ConvolutionalRecognitionProcessor.defaultFirstLayerNeurons();
        this.secondLayerNeurons = ConvolutionalRecognitionProcessor.defaultSecondLayerNeurons();
        this.config.setFirstLayerFilterCount(firstLayerNeurons.size());
        this.config.setSecondLayerFilterCount(secondLayerNeurons.size());
    }

    public RecognitionNetworkConfig getConfig() { return config; }
    public void setConfig(RecognitionNetworkConfig config) {
        this.config = config == null ? RecognitionNetworkConfig.fpv1080p() : config;
    }
    public List<ConvolutionalPerceptronNeuron> getFirstLayerNeurons() { return firstLayerNeurons; }
    public void setFirstLayerNeurons(List<ConvolutionalPerceptronNeuron> firstLayerNeurons) {
        this.firstLayerNeurons = firstLayerNeurons == null ? List.of() : List.copyOf(firstLayerNeurons);
        this.config.setFirstLayerFilterCount(this.firstLayerNeurons.size());
    }
    public List<ConvolutionalPerceptronNeuron> getSecondLayerNeurons() { return secondLayerNeurons; }
    public void setSecondLayerNeurons(List<ConvolutionalPerceptronNeuron> secondLayerNeurons) {
        this.secondLayerNeurons = secondLayerNeurons == null ? List.of() : List.copyOf(secondLayerNeurons);
        this.config.setSecondLayerFilterCount(this.secondLayerNeurons.size());
    }
    public Map<TargetClassification, ClassificationNeuron> getClassifierNeurons() { return classifierNeurons; }
    public void setClassifierNeurons(Map<TargetClassification, ClassificationNeuron> classifierNeurons) {
        this.classifierNeurons = new EnumMap<>(TargetClassification.class);
        if (classifierNeurons != null) {
            this.classifierNeurons.putAll(classifierNeurons);
        }
    }
    public int getLearningUpdates() { return learningUpdates; }

    public void remember(FeatureVectorSignal vector) {
        if (vector == null || vector.getFrameId() == null) {
            return;
        }
        recentFeatureVectors.put(vector.getFrameId(), vector);
        if (recentFeatureVectors.size() > 32) {
            String oldest = recentFeatureVectors.keySet().iterator().next();
            recentFeatureVectors.remove(oldest);
        }
    }

    public FeatureVectorSignal recentVector(String frameId) {
        return recentFeatureVectors.get(frameId);
    }

    public RecognitionLearningResultSignal applyFeedback(RecognitionFeedbackSignal feedback) {
        RecognitionLearningResultSignal result = new RecognitionLearningResultSignal();
        result.setMissionId(feedback.getMissionId());
        result.setUavId(feedback.getUavId());
        result.setTick(feedback.getTick());
        result.setFrameId(feedback.getFrameId());
        result.setTargetId(feedback.getTargetId());
        result.setOutcome(feedback.getOutcome());
        result.setReward(feedback.getReward());

        Map<String, Double> features = feedback.getImageFeatures();
        if (features.isEmpty()) {
            FeatureVectorSignal remembered = recentVector(feedback.getFrameId());
            if (remembered != null) {
                features = remembered.getFeatures();
            }
        }
        if (features.isEmpty()) {
            result.setReason("NO_FEATURE_VECTOR");
            return result;
        }

        double baseRate = feedback.getLearningRate() > 0.0 ? feedback.getLearningRate() : config.getLearningRate();
        double rate = Math.min(0.35, Math.max(0.0, baseRate * Math.max(0.25, Math.abs(feedback.getReward()))));
        TargetClassification expected = feedback.getExpectedClassification();
        TargetClassification predicted = feedback.getPredictedClassification();
        int updates = 0;
        Map<String, Object> matrices = new LinkedHashMap<>();

        if (expected != null && classifierNeurons.containsKey(expected)) {
            classifierNeurons.get(expected).adjustPrototype(features, rate);
            matrices.put("toward", expected.name());
            updates++;
        }
        if (predicted != null && expected != predicted && classifierNeurons.containsKey(predicted)) {
            classifierNeurons.get(predicted).repelPrototype(features, rate * 0.5);
            matrices.put("away", predicted.name());
            updates++;
        }
        learningUpdates += updates;
        result.setUpdatedMatrices(updates);
        result.setReason(updates == 0 ? "NO_CLASSIFIER_MATCH" : "CLASSIFIER_PROTOTYPE_MATRIX_UPDATED");
        result.getAttributes().put("matrixUpdates", matrices);
        result.getAttributes().put("learningUpdatesTotal", learningUpdates);
        return result;
    }
}
