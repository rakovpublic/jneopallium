package com.rakovpublic.jneuropallium.worker.demo.uavsingle.movement;

import com.rakovpublic.jneuropallium.worker.demo.uavsingle.UavSingleSignal;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of one movement decision: the winning action neuron, its score, the processor-produced
 * feature vector that drove it, the top competing neurons (for transparency) and the concrete
 * {@link MotorCommand} to execute.
 */
public class MovementDecisionSignal extends UavSingleSignal {
    private long frame;
    private double elapsedSeconds;
    private String neuronId;
    private String actionId;
    private String actionLabel;
    private double score;
    private final Map<String, Double> features = new LinkedHashMap<>();
    private final Map<String, Object> harmAssessment = new LinkedHashMap<>();
    private final List<Map<String, Object>> topNeuronScores = new ArrayList<>();
    private MotorCommand command;

    public MovementDecisionSignal() {
        setEventType("MOVEMENT_DECISION");
    }

    public long getFrame() { return frame; }
    public void setFrame(long frame) { this.frame = frame; }
    public double getElapsedSeconds() { return elapsedSeconds; }
    public void setElapsedSeconds(double elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
    public String getNeuronId() { return neuronId; }
    public void setNeuronId(String neuronId) { this.neuronId = neuronId; }
    public String getActionId() { return actionId; }
    public void setActionId(String actionId) { this.actionId = actionId; }
    public String getActionLabel() { return actionLabel; }
    public void setActionLabel(String actionLabel) { this.actionLabel = actionLabel; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public Map<String, Double> getFeatures() { return features; }
    public Map<String, Object> getHarmAssessment() { return harmAssessment; }
    public List<Map<String, Object>> getTopNeuronScores() { return topNeuronScores; }
    public MotorCommand getCommand() { return command; }
    public void setCommand(MotorCommand command) { this.command = command; }

    public Map<String, Object> asMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("frame", frame);
        map.put("elapsedSeconds", round(elapsedSeconds));
        map.put("neuronId", neuronId);
        map.put("actionId", actionId);
        map.put("actionLabel", actionLabel);
        map.put("score", round(score));
        Map<String, Object> roundedFeatures = new LinkedHashMap<>();
        features.forEach((key, value) -> roundedFeatures.put(key, round(value)));
        map.put("features", roundedFeatures);
        map.put("harmAssessment", harmAssessment);
        map.put("topNeuronScores", topNeuronScores);
        if (command != null) {
            map.put("motorCommandSignal", command.asMap());
        }
        return map;
    }

    private static double round(double value) {
        return Math.round(value * 1_000_000.0) / 1_000_000.0;
    }
}
