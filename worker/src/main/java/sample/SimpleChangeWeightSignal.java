package sample;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class SimpleChangeWeightSignal implements ISignal<Double> {
    private final Double value;
    private int sourceLayerId;
    private Long sourceNeuronId;
    private final static String DESCRIPTION = "Simple change weight signal";


    public SimpleChangeWeightSignal(Double value, int sourceLayerId, Long sourceNeuronId) {
        this.value = value;
        this.sourceLayerId = sourceLayerId;
        this.sourceNeuronId = sourceNeuronId;
    }

    @Override
    public Double getValue() {
        return value;
    }

    @Override
    public Class<? extends ISignal<Double>> getCurrentSignalClass() {
        return SimpleChangeWeightSignal.class;
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
        return DESCRIPTION;
    }

    @Override
    public boolean canUseProcessorForParent() {
        return false;
    }

    @Override
    public ISignal<Double> prepareSignalToNextStep() {
        return  new SimpleChangeWeightSignal( value/2,  sourceLayerId, sourceNeuronId) ;
    }

    @Override
    public int getSourceLayerId() {
        return sourceLayerId;
    }

    @Override
    public void setSourceLayerId(int layerId) {
        sourceLayerId = layerId;
    }

    @Override
    public Long getSourceNeuronId() {
        return sourceNeuronId;
    }

    @Override
    public void setSourceNeuronId(Long neuronId) {
        sourceNeuronId = neuronId;
    }

    @Override
    public int getTimeAlive() {
        return 0;
    }
}
