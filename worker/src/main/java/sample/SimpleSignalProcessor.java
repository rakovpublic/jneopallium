package sample;

import net.neuron.INeuron;
import net.neuron.ISignalProcessor;
import net.signals.ISignal;

import java.util.LinkedList;
import java.util.List;

public class SimpleSignalProcessor implements ISignalProcessor<SimpleSignal> {
    private String description="test";
    private  Class<? extends ISignalProcessor>  signalProcessorClass=SimpleSignalProcessor.class;

    public SimpleSignalProcessor() {
    }

    @Override
    public <I extends ISignal> List<I> process(SimpleSignal input, INeuron neuron) {
        List<ISignal> result= new LinkedList<>();
        result.add(input);
        return (List<I>) result;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public Boolean hasMerger() {
        return true;
    }

    @Override
    public Class<? extends ISignalProcessor> getSignalProcessorClass() {
        return signalProcessorClass;
    }
}
