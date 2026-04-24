/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SyscallSignal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Layer 1 process-behaviour matcher. Fires when a PID's recent syscall
 * sequence contains any configured forbidden subsequence (e.g.
 * CreateRemoteThread + VirtualAllocEx + WriteProcessMemory).
 * Loop=1 / Epoch=1.
 */
public class ProcessBehaviourNeuron extends ModulatableNeuron implements IProcessBehaviourNeuron {

    public static final class Rule {
        final String id;
        final int[] sequence;
        Rule(String id, int[] sequence) { this.id = id; this.sequence = sequence.clone(); }
    }

    private final List<Rule> rules = new ArrayList<>();
    private final Map<Integer, Deque<Integer>> windows = new HashMap<>();
    private int windowSize = 32;

    public ProcessBehaviourNeuron() { super(); }
    public ProcessBehaviourNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    public void setWindowSize(int w) { this.windowSize = Math.max(2, w); }

    @Override
    public void addForbiddenSequence(String id, int[] syscallNums) {
        if (id == null || syscallNums == null || syscallNums.length == 0) return;
        rules.add(new Rule(id, syscallNums));
    }

    @Override
    public SignatureMatchSignal observe(SyscallSignal s) {
        if (s == null) return null;
        Deque<Integer> w = windows.computeIfAbsent(s.getPid(), k -> new ArrayDeque<>());
        w.addLast(s.getSyscallNum());
        while (w.size() > windowSize) w.removeFirst();
        for (Rule r : rules) {
            if (containsSequence(w, r.sequence)) {
                return new SignatureMatchSignal(r.id, "process-behaviour", 0.85,
                        "pid=" + s.getPid() + " proc=" + s.getProcName());
            }
        }
        return null;
    }

    @Override
    public int ruleCount() { return rules.size(); }

    @Override
    public void resetFor(int pid) { windows.remove(pid); }

    private static boolean containsSequence(Deque<Integer> window, int[] sequence) {
        int[] arr = new int[window.size()];
        int i = 0;
        for (Integer v : window) arr[i++] = v;
        int matched = 0;
        for (int v : arr) {
            if (v == sequence[matched]) {
                matched++;
                if (matched == sequence.length) return true;
            }
        }
        return false;
    }
}
