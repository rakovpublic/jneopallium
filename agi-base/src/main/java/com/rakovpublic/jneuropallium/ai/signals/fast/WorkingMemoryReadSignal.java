package com.rakovpublic.jneuropallium.ai.signals.fast;

import com.rakovpublic.jneuropallium.ai.signals.BaseSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class WorkingMemoryReadSignal extends BaseSignal {
    private String slotId;
    private String callbackNeuronId;
    private Object result; // set when returning result

    public WorkingMemoryReadSignal() { super(); this.loop = 1; this.epoch = 2L; }
    public WorkingMemoryReadSignal(String slotId, String callbackNeuronId) {
        this(); this.slotId = slotId; this.callbackNeuronId = callbackNeuronId;
    }

    public String getSlotId() { return slotId; }
    public void setSlotId(String slotId) { this.slotId = slotId; }
    public String getCallbackNeuronId() { return callbackNeuronId; }
    public void setCallbackNeuronId(String callbackNeuronId) { this.callbackNeuronId = callbackNeuronId; }
    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    @Override public Class<? extends ISignal<Void>> getCurrentSignalClass() { return WorkingMemoryReadSignal.class; }
    @Override public String getDescription() { return "WorkingMemoryReadSignal"; }
    @Override public <K extends ISignal<Void>> K copySignal() {
        WorkingMemoryReadSignal c = new WorkingMemoryReadSignal(slotId, callbackNeuronId);
        c.result = this.result;
        c.sourceLayer = this.sourceLayer; c.sourceNeuron = this.sourceNeuron;
        c.loop = this.loop; c.epoch = this.epoch; c.name = this.name;
        return (K) c;
    }
}
