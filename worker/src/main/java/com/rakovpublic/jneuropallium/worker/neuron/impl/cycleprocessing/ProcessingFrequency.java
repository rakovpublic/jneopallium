package com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing;

public class ProcessingFrequency {
  private Long epoch;
  private Integer loop;

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
