/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.net.neuron.impl.security;

import com.rakovpublic.jneuropallium.ai.neurons.base.ModulatableNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalChain;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.LogEventSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.PacketSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.impl.security.SignatureMatchSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Layer 1 signature matcher. Maintains a list of immutable rules and
 * emits a {@link SignatureMatchSignal} on the first hit. This is a naive
 * substring matcher — a production deployment plugs Hyperscan or
 * Aho-Corasick behind the same interface.
 * Loop=1 / Epoch=1.
 */
public class SignaturePatternNeuron extends ModulatableNeuron implements ISignaturePatternNeuron {

    /** Immutable signature record. */
    public static final class Rule {
        final String signatureId;
        final String family;
        final byte[] pattern;
        final String referenceIoc;
        Rule(String id, String fam, byte[] pat, String ioc) {
            this.signatureId = id; this.family = fam; this.pattern = pat.clone(); this.referenceIoc = ioc;
        }
    }

    private final List<Rule> rules = new ArrayList<>();

    public SignaturePatternNeuron() { super(); }
    public SignaturePatternNeuron(Long neuronId, ISignalChain chain, Long run) { super(neuronId, chain, run); }

    @Override
    public void addSignature(String signatureId, String family, byte[] pattern, String referenceIoc) {
        if (signatureId == null || pattern == null) return;
        rules.add(new Rule(signatureId, family, pattern, referenceIoc));
    }

    @Override
    public SignatureMatchSignal match(PacketSignal p) {
        if (p == null) return null;
        byte[] body = p.getSummary();
        if (body == null) return null;
        for (Rule r : rules) {
            if (contains(body, r.pattern)) {
                return new SignatureMatchSignal(r.signatureId, r.family, 0.9, r.referenceIoc);
            }
        }
        return null;
    }

    @Override
    public SignatureMatchSignal match(LogEventSignal l) {
        if (l == null) return null;
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : l.getFields().entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append(' ');
        }
        byte[] body = sb.toString().getBytes();
        for (Rule r : rules) {
            if (contains(body, r.pattern)) {
                return new SignatureMatchSignal(r.signatureId, r.family, 0.8, r.referenceIoc);
            }
        }
        return null;
    }

    @Override
    public int signatureCount() { return rules.size(); }

    private static boolean contains(byte[] haystack, byte[] needle) {
        if (needle.length == 0 || haystack.length < needle.length) return false;
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
