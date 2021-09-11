package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.neuron.INConnection;
import com.rakovpublic.jneuropallium.worker.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.neuron.IRule;
import com.rakovpublic.jneuropallium.worker.neuron.impl.NeuronRunnerService;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.impl.layersizing.CreateNeuronSignal;

import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public class Layer implements ILayer {
    protected TreeMap<Long, INeuron> map;
    private HashMap<Long, List<ISignal>> input;
    private HashMap<Class<? extends INeuron>, INeuronSerializer> neuronSerializerHashMap;
    private Boolean isProcessed;
    private long size;
    private int layerId;
    private LinkedBlockingQueue<INeuron> notProcessed;
    private List<IRule> rules;
    private IInputResolver inputResolver;


    public Layer(int layerId, IInputResolver meta) {
        neuronSerializerHashMap = new HashMap<>();
        rules = new ArrayList<>();
        isProcessed = false;
        notProcessed = new LinkedBlockingQueue<INeuron>();
        this.layerId = layerId;
        inputResolver = meta;
        map = new TreeMap<Long, INeuron>();
        input = new HashMap<Long, List<ISignal>>();
    }


    @Override
    public synchronized <K extends CreateNeuronSignal> void createNeuron(K signal) {
        INeuron newNeuron= signal.getValue().getNeuron();
        if(!map.containsKey(newNeuron.getId())&&newNeuron.getId()!=null){
            map.put(newNeuron.getId(),newNeuron);
        }else {
            Long newId=map.lastKey()+1;
            newNeuron.setId(newId);
            for(Object signals: signal.getValue().getCreateRelationsSignals().values()){
                HashMap<Long, List<ISignal>> sMap = (HashMap<Long, List<ISignal>>)signals;
                for(List<ISignal> val:sMap.values()){
                    for(ISignal sig:val){
                        sig.setSourceNeuronId(newId);
                    }
                }
            }

        }
        inputResolver.getSignalPersistStorage().putSignals(signal.getValue().getCreateRelationsSignals());
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
    public void registerAll(List<? extends INeuron> neuron) {
        for (INeuron ner : neuron) {
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
        HashMap<Long, List<ISignal>> inputs = inputResolver.getSignalPersistStorage().getLayerSignals(this.layerId);
        for (Long neuronID : inputs.keySet()) {
            for (ISignal signal : inputs.get(neuronID)) {
                ISignal nextStepSignal = signal.prepareSignalToNextStep();
                if (nextStepSignal != null) {
                    this.addInput(signal, neuronID);
                }
            }
        }
        INeuron neur;
        NeuronRunnerService ns = NeuronRunnerService.getService();
        for (Long neuronId : map.keySet()) {
            if (input.containsKey(neuronId)) {
                neur = map.get(neuronId);
                neur.setCurrentLoopAmount(inputResolver.getCurrentLoop());
                neur.setCyclingNeuronInputMapping(inputResolver.getCycleNeuronAddressMapping());
                neur.setSignalHistory(inputResolver.getSignalsHistoryStorage());
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
    public void dumpResult() {
        HashMap<Integer, HashMap<Long, List<ISignal>>> result = getResults();
        inputResolver.getSignalPersistStorage().putSignals(result);
    }

    @Override
    public void dumpNeurons(ILayerMeta layerMeta) {
        List<INeuron> neurons = new LinkedList<>();
        for(INeuron n:map.values()){
            if(!n.getAxon().isConnectionsWrapped()){
                n.getAxon().wrapConnections();
            }
            neurons.add(n);
        }
        layerMeta.saveNeurons(neurons);
        layerMeta.dumpLayer();

    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getResults() {
        HashMap<Integer, HashMap<Long, List<ISignal>>> result = new HashMap<>();
        for (Long neurId : map.keySet()) {
            INeuron neur = map.get(neurId);
            IAxon axon = neur.getAxon();
            if(axon.isConnectionsWrapped()){
                axon.unwrapConnections();
            }
            HashMap<ISignal, List<INConnection>> tMap = axon.processSignals(neur.getResult());
            for (ISignal signal : tMap.keySet()) {
                signal.setSourceLayerId(this.layerId);
                signal.setSourceNeuronId(neurId);
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
        neuronSerializerHashMap.put(serializer.getDeserializedClass(), serializer);

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
