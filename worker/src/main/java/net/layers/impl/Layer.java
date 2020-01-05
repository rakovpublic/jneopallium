package net.layers.impl;

import net.layers.ILayer;
import net.neuron.IAxon;
import net.neuron.INConnection;
import net.neuron.INeuron;
import net.neuron.IRule;
import net.neuron.impl.NeuronRunnerService;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ILayerMeta;
import net.storages.INeuronSerializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public class Layer implements ILayer {
    private HashMap<Long, INeuron> map;
    private HashMap<Long, List<ISignal>> input;
    private HashMap<Class<?extends INeuron>,INeuronSerializer> neuronSerializerHashMap;
    private Boolean isProcessed;
    private long size;
    private int layerId;
    private List<INeuron> notProcessed;
    private List<IRule> rules;


    public Layer(int layerId) {
        neuronSerializerHashMap= new HashMap<>();
        rules = new ArrayList<>();
        isProcessed = false;
        notProcessed = new ArrayList<INeuron>();
        this.layerId = layerId;
        map = new HashMap<Long, INeuron>();
        input = new HashMap<Long, List<ISignal>>();
    }



    @Override
    public long getLayerSize() {
        return size;
    }

    @Override
    public Boolean validateGlobal() {
        for (IRule r : rules) {
            for (INeuron neuron : map.values()) {
                if (!r.validate(neuron)) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Boolean validateLocal() {
        for (INeuron neuron : map.values()) {
            if (!neuron.validate()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void addGlobalRule(IRule rule) {
        rules.add(rule);
    }

    @Override
    public void register(INeuron neuron) {
        map.put(neuron.getId(), neuron);

    }

    @Override
    public void registerAll(List<INeuron> neuron) {
        for(INeuron ner:neuron){
            map.put(ner.getId(), ner);
        }
    }


    @Override
    public void addInput(ISignal signal, Long neuronId) {

        if (input.containsKey(neuronId)) {
            input.get(neuronId).add(signal);
        } else {
            List<ISignal> list = new ArrayList<>();
            list.add(signal);
            input.put(neuronId, list);
        }
    }

    @Override
    public void process() {
        INeuron neur;
        NeuronRunnerService ns = NeuronRunnerService.getService();
        for (Long neuronId : map.keySet()) {
            if (input.containsKey(neuronId)) {
                neur = map.get(neuronId);
                neur.addSignals(input.get(neuronId));
                ns.addNeuron(neur);
            }
        }
        ns.process();

    }

    @Override
    public int getId() {
        return layerId;
    }

    @Override
    public Boolean isProcessed() {

        if (!isProcessed && notProcessed.size() == 0) {
            isProcessed = true;
            for (INeuron ner : map.values()) {
                if (!ner.hasResult()) {
                    notProcessed.add(ner);
                    isProcessed = false;
                }
            }
        } else {

            for (INeuron ner : notProcessed) {
                if (ner.hasResult()) {
                    notProcessed.remove(ner);
                }
            }
            if (notProcessed.size() == 0) {
                isProcessed = true;
            }
        }
        return isProcessed;
    }

    @Override
    public void dumpResult(IInputMeta meta) {
        HashMap<Integer, HashMap<Long, List<ISignal>>> result = getResults();
        for (int layerId : result.keySet()) {
            meta.saveResults(result.get(layerId), layerId);
        }

    }

    @Override
    public void dumpNeurons(ILayerMeta layerMeta) {
        layerMeta.saveNeurons(map.values());
        layerMeta.dumpLayer();

    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getResults() {
        HashMap<Integer, HashMap<Long, List<ISignal>>> result = new HashMap<>();
        for (Long neurId : map.keySet()) {
            INeuron neur = map.get(neurId);
            IAxon axon = neur.getAxon();
            HashMap<ISignal, List<INConnection>> tMap = axon.processSignals(neur.getResult());
            for (ISignal signal : tMap.keySet()) {
                for (INConnection connection : tMap.get(signal)) {
                    int layerId = connection.getTargetLayerId();
                    Long targetNeurId = connection.getTargetNeuronId();
                    if (result.containsKey(layerId)) {
                        if (result.get(layerId).containsKey(targetNeurId)) {
                            result.get(layerId).get(targetNeurId).add(signal);
                        } else {
                            List<ISignal> signals = new ArrayList<>();
                            signals.add(signal);
                            result.get(layerId).put(targetNeurId, signals);
                        }
                    } else {
                        List<ISignal> signals = new ArrayList<>();
                        signals.add(signal);
                        HashMap<Long, List<ISignal>> ttMap = new HashMap<>();
                        ttMap.put(targetNeurId, signals);
                        result.put(layerId, ttMap);


                    }
                }
            }
        }
        return result;
    }


    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public void addNeuronSerializer(INeuronSerializer serializer) {
        neuronSerializerHashMap.put(serializer.getDeserializedClass(),serializer);

    }

    @Override
    public <N extends INeuron> INeuronSerializer<N> getNeuronSerializer(Class<N> neuronClass) {
        return neuronSerializerHashMap.get(neuronClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Layer layer = (Layer) o;
        return layerId == layer.layerId &&
                Objects.equals(map, layer.map) &&
                Objects.equals(input, layer.input) &&
                Objects.equals(isProcessed, layer.isProcessed) &&
                Objects.equals(notProcessed, layer.notProcessed) &&
                Objects.equals(rules, layer.rules);
    }

    @Override
    public int hashCode() {

        return Objects.hash(map, input, isProcessed, notProcessed, rules, layerId);
    }
}
