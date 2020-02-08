package sample;

import net.signals.ISignal;

public class SimpleChangeWeightSignal implements ISignal<Double> {
    @Override
    public Double getValue() {
        return null;
    }

    @Override
    public Class<? extends ISignal<Double>> getCurrentClass() {
        return null;
    }

    @Override
    public Class<Double> getParamClass() {
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
    public ISignal<Double> prepareSignalToNextStep() {
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
