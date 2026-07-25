package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class WorkingMemoryWriteSignal extends BaseSignal {
    private String slotId;
    private Object content;
    private int ttl;

    public WorkingMemoryWriteSignal() { super(); this.loop = 1; this.epoch = 1L; }
    public WorkingMemoryWriteSignal(String slotId, Object content, int ttl) {
        this(); this.slotId = slotId; this.content = content; this.ttl = ttl;
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public Object getContent() { return content; }
    public void setContent(Object content) { this.content = content; }
    public int getTtl() { return ttl; }
    public void setTtl(int ttl) { this.ttl = ttl; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return WorkingMemoryWriteSignal.class; }
    @Override public String getDescription() { return "WorkingMemoryWriteSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        WorkingMemoryWriteSignal c = new WorkingMemoryWriteSignal(slotId, content, ttl);
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
