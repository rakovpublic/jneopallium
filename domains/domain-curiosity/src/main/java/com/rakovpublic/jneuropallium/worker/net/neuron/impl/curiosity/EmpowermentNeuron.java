/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.curiosity;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.curiosity.EmpowermentSignal;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Estimates empowerment — mutual information between the agent's action
 * choice and future state — using a plug-in estimator over sampled action
 * rollouts provided by a forward model (depends on embodiment module's
 * {@code EfferenceCopySignal}).
 * Layer 4, loop=2 / epoch=3.
 * <p>Biological analogue: prefrontal computation of controllability,
 * aligned with Klyubin et al. (2005) "empowerment" measure.
 */
public class EmpowermentNeuron extends ModulatableNeuron implements IEmpowermentNeuron {

    private int horizon = 3;
    private int nActionSamples = 8;

    public EmpowermentNeuron() { super(); }

    public EmpowermentNeuron(Long neuronId, ISignalChain chain, Long run) {
        super(neuronId, chain, run);
    }

    /**
     * Estimate mutual information I(A;S') by counting distinct reachable
     * future-state bins from the supplied action rollouts.
     *
     * @param stateId current discrete state id
     * @param rollouts each entry is a list of reachable future-state ids
     *                 when that action is taken
     * @return an {@link EmpowermentSignal}
     */
    public EmpowermentSignal estimate(int stateId, List<List<Integer>> rollouts) {
        if (rollouts == null || rollouts.isEmpty()) {
            return finalSignal(stateId, 0.0);
        }
        Set<Integer> reachable = new HashSet<>();
        int totalFutures = 0;
        int actionCount = 0;
        for (List<Integer> futures : rollouts) {
            if (futures == null || futures.isEmpty()) continue;
            actionCount++;
            for (Integer f : futures) {
                if (f != null) {
                    reachable.add(f);
                    totalFutures++;
                }
            }
        }
        if (actionCount < 2 || totalFutures == 0) {
            return finalSignal(stateId, 0.0);
        }
        double avgFuturesPerAction = (double) totalFutures / actionCount;
        double distinct = reachable.size();
        double mi = Math.log(distinct) - Math.log(avgFuturesPerAction);
        if (mi < 0) mi = 0;
        if (distinct > 0 && actionCount > 0) {
            mi += Math.log(Math.min(distinct, actionCount)) * 0.5;
        }
        return finalSignal(stateId, mi);
    }

    private EmpowermentSignal finalSignal(int stateId, double mi) {
        EmpowermentSignal s = new EmpowermentSignal(stateId, mi);
        s.setSourceNeuronId(this.getId());
        return s;
    }

    public int getHorizon() { return horizon; }
    public void setHorizon(int horizon) { this.horizon = Math.max(1, horizon); }
    public int getNActionSamples() { return nActionSamples; }
    public void setNActionSamples(int nActionSamples) { this.nActionSamples = Math.max(1, nActionSamples); }
}
