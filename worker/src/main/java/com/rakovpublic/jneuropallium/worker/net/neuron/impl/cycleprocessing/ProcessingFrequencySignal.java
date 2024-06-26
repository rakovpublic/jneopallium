package com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing;

import com.rakovpublic.jneuropallium.worker.net.signals.AbstractSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;

public class ProcessingFrequencySignal extends AbstractSignal<ProcessingFrequencySignalItem> implements ISignal<ProcessingFrequencySignalItem> {

    public ProcessingFrequencySignal(ProcessingFrequencySignalItem value, Integer sourceLayer, Long sourceNeuron, Integer timeAlive, String description, boolean fromExternalNet, String inputName, boolean needToRemoveDuringLearning, boolean needToProcessDuringLearning, String name) {
        super(value, sourceLayer, sourceNeuron, timeAlive, description, fromExternalNet, inputName, needToRemoveDuringLearning, needToProcessDuringLearning, ProcessingFrequencySignal.class.getName(), ProcessingFrequencySignal.class.getCanonicalName());
    }

    @Override
    public Class<? extends ISignal<ProcessingFrequencySignalItem>> getCurrentSignalClass() {
        return ProcessingFrequencySignal.class;
    }

    @Override
    public Class<ProcessingFrequencySignalItem> getParamClass() {
        return ProcessingFrequencySignalItem.class;
    }

    @Override
    public ProcessingFrequencySignal copySignal() {
        return new ProcessingFrequencySignal(value, this.getSourceLayerId(), this.getSourceNeuronId(), this.getTimeAlive(), getDescription(), isFromExternalNet(), getInputName(), this.isNeedToRemoveDuringLearning(), this.isNeedToProcessDuringLearning(), ProcessingFrequency.class.getName());
    }
}
