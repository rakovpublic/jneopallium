package com.rakovpublic.jneuropallium.ai.processors;

import com.rakovpublic.jneuropallium.ai.neurons.learning.STDPNeuron;
import com.rakovpublic.jneuropallium.ai.neurons.learning.ISTDPNeuron;
import com.rakovpublic.jneuropallium.ai.signals.fast.SpikeSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

import java.util.ArrayList;
import java.util.List;

public class STDPProcessor implements ISignalProcessor<SpikeSignal, ISTDPNeuron> {

    @Override
    public <I extends ISignal> List<I> process(SpikeSignal input, ISTDPNeuron neuron) {
        long now = System.currentTimeMillis();
        String sourceId = input.getSourceNeuronId() != null ? input.getSourceNeuronId().toString() : "unknown";
        neuron.getPreSpikeTimestamps().put(sourceId, now);
        // Simulate post-spike: apply LTP for pre before post (causal), LTD for pre after post (anti-causal)
        if (input.isFired()) {
            for (java.util.Map.Entry<String, Long> entry : neuron.getPreSpikeTimestamps().entrySet()) {
                long preTime = entry.getValue();
                long tDiff = now - preTime;
                if (tDiff >= 0 && tDiff < neuron.getStdpWindow()) {
                    // LTP: pre before post
                    neuron.adjustWeight(entry.getKey(), neuron.getLtpRate());
                } else if (tDiff < 0 && Math.abs(tDiff) < neuron.getStdpWindow()) {
                    // LTD: pre after post
                    neuron.adjustWeight(entry.getKey(), -neuron.getLtdRate());
                }
            }
        }
        return new ArrayList<>();
    }

    @Override public String getDescription() { return "STDPProcessor"; }
    @Override public Boolean hasMerger() { return false; }
    @Override public Class<? extends ISignalProcessor> getSignalProcessorClass() { return STDPProcessor.class; }
    @Override public Class<ISTDPNeuron> getNeuronClass() { return ISTDPNeuron.class; }
    @Override public Class<SpikeSignal> getSignalClass() { return SpikeSignal.class; }
}
