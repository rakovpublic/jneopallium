package com.rakovpublic.jneuropallium.ai.model;

import java.util.Arrays;

/** Simple linear forward model for predictive coding. */
public class InternalForwardModel {
    private double[] state;
    private double[][] transitionMatrix;

    public InternalForwardModel(int stateDimensions) {
        state = new double[stateDimensions];
        transitionMatrix = new double[stateDimensions][stateDimensions];
        // Initialize as identity
        for (int i = 0; i < stateDimensions; i++) transitionMatrix[i][i] = 1.0;
    }

    public void updateState(double[] contextVector) {
        if (contextVector != null && contextVector.length > 0) {
            int len = Math.min(contextVector.length, state.length);
            System.arraycopy(contextVector, 0, state, 0, len);
        }
    }

    public double[] predict(double[] currentState) {
        int n = transitionMatrix.length;
        double[] predicted = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0;
            for (int j = 0; j < Math.min(n, currentState.length); j++) {
                sum += transitionMatrix[i][j] * currentState[j];
            }
            predicted[i] = sum;
        }
        return predicted;
    }

    public double[] getState() { return Arrays.copyOf(state, state.length); }
}
