package sample;

import com.rakovpublic.jneuropallium.worker.neuron.INConnection;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.study.IStudyingRequest;

import java.util.HashMap;
import java.util.List;

public class SimpleStudyingRequest implements IStudyingRequest {
    private final int layerId;
    private final long neuronId;
    private final HashMap<Class<? extends ISignal>, List<INConnection>> results;

    public SimpleStudyingRequest(int layerId, long neuronId, HashMap<Class<? extends ISignal>, List<INConnection>> results) {
        this.layerId = layerId;
        this.neuronId = neuronId;
        this.results = results;
    }

    @Override
    public int getLayerId() {
        return layerId;
    }

    @Override
    public Long getNeuronId() {
        return neuronId;
    }

    @Override
    public HashMap<Class<? extends ISignal>, List<INConnection>> getNewConnections() {
        return results;
    }

    @Override
    public String toJSON() {
        return null;
    }
}
