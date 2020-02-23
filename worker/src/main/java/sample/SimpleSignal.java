package sample;

import net.signals.ISignal;

public class SimpleSignal implements ISignal<Double> {
    private Double value;
    private int timeAlive;
    private int layerId;
    private Long neuronId;

    public SimpleSignal(Double value, int timeAlive, int layerId, Long neuronId) {
        this.value = value;
        this.timeAlive = timeAlive;
        this.layerId = layerId;
        this.neuronId = neuronId;
    }

    public SimpleSignal(Double value, int timeAlive) {
        this.value = value;
        this.timeAlive = timeAlive;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public Class<? extends ISignal> getCurrentClass() {
        return SimpleSignal.class;
    }

    @Override
    public Class getParamClass() {
        return Double.class;
    }

    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public String getDescription() {
        return "Simple signal";
    }

    @Override
    public boolean canUseProcessorForParent() {
        return false;
    }

    @Override
    public ISignal prepareSignalToNextStep() {
        if(timeAlive-1>0){
            return new SimpleSignal(value,timeAlive-1,layerId,neuronId);
        }
        return null;
    }

    @Override
    public int getSourceLayerId() {
        return layerId;
    }

    @Override
    public void setSourceLayerId(int layerId) {
        this.layerId=layerId;
    }

    @Override
    public Long getSourceNeuronId() {
        return neuronId;
    }

    @Override
    public void setSourceNeuronId(Long neuronId) {
        this.neuronId=neuronId;
    }
}
