/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.ActuatorCommandSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.industrial.SetpointSignal;

/**
 * Layer 4 model-predictive controller. Samples a set of candidate
 * control moves across the control horizon, evaluates each against
 * the {@link IProcessModelNeuron} forecast over the prediction
 * horizon, and emits the first move of the best-scoring trajectory.
 * Loop=1 / Epoch=3.
 */
public class MPCPlanningNeuron extends ModulatableNeuron implements IMPCPlanningNeuron {

    private int horizon = 60;
    private int controlHorizon = 10;
    private IProcessModelNeuron model;

    public MPCPlanningNeuron() { super(); }
    public MPCPlanningNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override public void setHorizon(int horizonTicks) { this.horizon = Math.max(1, horizonTicks); }
    @Override public void setControlHorizon(int controlHorizonTicks) {
        this.controlHorizon = Math.max(1, Math.min(this.horizon, controlHorizonTicks));
    }
    @Override public void setProcessModel(IProcessModelNeuron model) { this.model = model; }
    @Override public int getHorizon() { return horizon; }
    @Override public int getControlHorizon() { return controlHorizon; }

    @Override
    public ActuatorCommandSignal step(SetpointSignal sp, double currentValue) {
        if (sp == null) return null;
        double bestMove = currentValue;
        double bestCost = Double.POSITIVE_INFINITY;
        for (double move = -1.0; move <= 1.0; move += 0.25) {
            double predicted = (model == null)
                    ? currentValue + move
                    : currentValue + model.predict(sp.getTag(), move, horizon);
            double err = sp.getSetpoint() - predicted;
            double cost = err * err + 0.05 * move * move;
            if (cost < bestCost) { bestCost = cost; bestMove = currentValue + move; }
        }
        return new ActuatorCommandSignal(sp.getTag(), bestMove, currentValue, true);
    }
}
