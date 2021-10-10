package sample;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;

import java.util.LinkedList;
import java.util.List;

public class SimpleSignalChain implements ISignalChain {
    private List<Class<? extends ISignal>> chaine;
    private String description;

    public SimpleSignalChain() {
        this.chaine = new LinkedList<>();
        chaine.add(SimpleSignal.class);
        description = "Simple test";
    }

    @Override
    public List<Class<? extends ISignal>> getProcessingChain() {
        return chaine;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
