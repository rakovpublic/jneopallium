package sample;

import net.neuron.ISignalMerger;

import java.util.List;

public class SimpleSignalMerger implements ISignalMerger<SimpleSignal> {
    @Override
    public SimpleSignal mergeSignals(List<SimpleSignal> signals) {
        double resultSignal=0;
        for(SimpleSignal ss:signals){
            resultSignal+=ss.getValue();
        }
        return new SimpleSignal(resultSignal,1);
    }

    @Override
    public String getDescription() {
        return null;
    }
}
