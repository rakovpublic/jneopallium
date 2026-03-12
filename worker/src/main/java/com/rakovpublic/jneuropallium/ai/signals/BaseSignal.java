package com.rakovpublic.jneuropallium.ai.signals;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public abstract class BaseSignal extends AbstractSignal<Void> {

    public BaseSignal() {
        super();
        this.timeAlive = 100;
        this.needToProcessDuringLearning = false;
        this.needToRemoveDuringLearning = false;
    }

    @Override
    public Void getValue() { return null; }

    @Override
    public Class<Void> getParamClass() { return Void.class; }

    @Override
    public String toJSON() { return "{}"; }

    @Override
    public boolean canUseProcessorForParent() { return false; }

    @Override
    public ISignal<Void> prepareSignalToNextStep() {
        if (this.timeAlive != null && this.timeAlive > 0) this.timeAlive--;
        return this;
    }
}
