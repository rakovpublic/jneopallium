package sample;

import net.signals.ISignal;

public class SimpleSignal implements ISignal {
    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Class<? extends ISignal> getCurrentClass() {
        return null;
    }

    @Override
    public Class getParamClass() {
        return null;
    }

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean canUseProcessorForParent() {
        return false;
    }

    @Override
    public ISignal prepareSignalToNextStep() {
        return null;
    }

    @Override
    public int getSourceLayerId() {
        return 0;
    }

    @Override
    public void setSourceLayerId(int layerId) {

    }

    @Override
    public Long getSourceNeuronId() {
        return null;
    }

    @Override
    public void setSourceNeuronId(Long neuronId) {

    }
}
