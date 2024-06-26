package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

public class ProcessingFrequency {
    public long epoch;
    public int loop;

    public ProcessingFrequency(Long epoch, Integer loop) {
        this.epoch = epoch;
        this.loop = loop;
    }

    public ProcessingFrequency() {
    }

    public Long getEpoch() {
        return epoch;
    }

    public void setEpoch(Long epoch) {
        this.epoch = epoch;
    }

    public Integer getLoop() {
        return loop;
    }

    public void setLoop(Integer loop) {
        this.loop = loop;
    }
}
