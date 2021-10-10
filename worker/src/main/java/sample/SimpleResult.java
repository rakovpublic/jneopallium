package sample;

import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class SimpleResult implements IResultSignal<Double> {
    public SimpleResult() {

    }

    private Class<? extends ISignal> currentSignalClass = SimpleResult.class;

    @Override
    public Double getResultObject() {
        return 1d;
    }

    @Override
    public Class<Double> getResultObjectClass() {
        return null;
    }

    @Override
    public Double getValue() {
        return 1d;
    }

    @Override
    public Class<? extends ISignal> getCurrentSignalClass() {
        return currentSignalClass;
    }

    @Override
    public Class<Double> getParamClass() {
        return Double.class;
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

    @Override
    public int getTimeAlive() {
        return 0;
    }
}
