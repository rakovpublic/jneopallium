package com.rakovpublic.jneuropallium.ai.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class WorldStateModel {
    private double[] environmentVector;
    private double[] humanStateVector; // 5 dimensions
    private Map<String, double[]> objectStates;

    public WorldStateModel() {
        environmentVector = new double[0];
        humanStateVector = new double[5];
        objectStates = new HashMap<>();
    }

    public WorldStateModel(double[] environmentVector, double[] humanStateVector, Map<String, double[]> objectStates) {
        this.environmentVector = environmentVector;
        this.humanStateVector = humanStateVector;
        this.objectStates = objectStates;
    }

    public double[] getEnvironmentVector() { return environmentVector; }
    public void setEnvironmentVector(double[] environmentVector) { this.environmentVector = environmentVector; }
    public double[] getHumanStateVector() { return humanStateVector; }
    public void setHumanStateVector(double[] humanStateVector) { this.humanStateVector = humanStateVector; }
    public Map<String, double[]> getObjectStates() { return objectStates; }
    public void setObjectStates(Map<String, double[]> objectStates) { this.objectStates = objectStates; }

    /** Returns a completely independent deep copy — no shared object references. */
    public WorldStateModel deepClone() {
        double[] envCopy = Arrays.copyOf(environmentVector, environmentVector.length);
        double[] humanCopy = Arrays.copyOf(humanStateVector, humanStateVector.length);
        Map<String, double[]> objCopy = new HashMap<>();
        for (Map.Entry<String, double[]> entry : objectStates.entrySet()) {
            objCopy.put(entry.getKey(), Arrays.copyOf(entry.getValue(), entry.getValue().length));
        }
        return new WorldStateModel(envCopy, humanCopy, objCopy);
    }
}
