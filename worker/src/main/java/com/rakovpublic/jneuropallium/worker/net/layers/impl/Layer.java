package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.IRule;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISynapse;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronRunnerService;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.CreateNeuronSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.DeleteNeuronSignal;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.LayerManipulatingNeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.layersizing.LayerManipulatingProcessingChain;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/***
 * Created by Rakovskyi Dmytro on 08.06.2018.
 */
public class Layer<N extends INeuron> implements ILayer<N> {
    protected TreeMap<Long, INeuron> map;
    private Boolean isProcessed;
    private int layerId;
    private ConcurrentLinkedQueue<INeuron> notProcessed;
    private List<IRule> rules;
    private IInputResolver inputResolver;
    private HashMap<String, LayerMetaParam> metaParams;
    private int threads;


    public Layer(int layerId, IInputResolver meta, int threads) {
        this.threads = threads;
        metaParams = new HashMap<>();
        rules = new ArrayList<>();
        isProcessed = false;
        notProcessed = new ConcurrentLinkedQueue<INeuron>();
        this.layerId = layerId;
        inputResolver = meta;
        map = new TreeMap<Long, INeuron>();
        INeuron sizingNeuron = new LayerManipulatingNeuron(Long.MIN_VALUE, new LayerManipulatingProcessingChain(), 0l, this);
        sizingNeuron.setLayer(this);
        map.put(Long.MIN_VALUE, sizingNeuron);
    }


    @Override
    public synchronized <K extends CreateNeuronSignal> void createNeuron(K signal) {
        INeuron newNeuron = signal.getValue().getNeuron();
        newNeuron.setLayer(this);
        if (!map.containsKey(newNeuron.getId()) && newNeuron.getId() != null) {
            map.put(newNeuron.getId(), newNeuron);
        } else {
            Long newId = map.lastKey() + 1;
            newNeuron.setId(newId);
            for (Object signals : signal.getValue().getCreateRelationsSignals().values()) {
                HashMap<Long, List<ISignal>> sMap = (HashMap<Long, List<ISignal>>) signals;
                for (List<ISignal> val : sMap.values()) {
                    for (ISignal sig : val) {
                        sig.setSourceNeuronId(newId);
                    }
                }
            }

        }
        inputResolver.getSignalPersistStorage().putSignals(signal.getValue().getCreateRelationsSignals());
    }

    @Override
    public LayerMetaParam getLayerMetaParam(String key) {
        return metaParams.get(key);
    }

    @Override
    public void updateLayerMetaParam(String key, LayerMetaParam metaParam) {
        metaParams.put(key, metaParam);
    }

    @Override
    public void setLayerMetaParams(HashMap<String, LayerMetaParam> params) {
        metaParams = params;
    }

    @Override
    public void deleteNeuron(DeleteNeuronSignal deleteNeuronIntegration) {
        map.remove(deleteNeuronIntegration.getValue().getNeuronId());
        inputResolver.getSignalPersistStorage().putSignals(deleteNeuronIntegration.getValue().getCreateRelationsSignals());
    }

    @Override
    public long getLayerSize() {
        return this.map.size();
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
        neuron.setLayer(this);
        map.put(neuron.getId(), neuron);

    }

    @Override
    public void registerAll(List<? extends N> neuron) {
        for (INeuron ner : neuron) {
            ner.setLayer(this);
            map.put(ner.getId(), ner);
        }
    }


    @Override
    public void process() {
        HashMap<Long, List<ISignal>> input = inputResolver.getSignalPersistStorage().getLayerSignals(this.layerId);
        INeuron neur;
        NeuronRunnerService ns = NeuronRunnerService.getService();
        notProcessed = ns.getNeuronQueue();
        for (Long neuronId : map.keySet()) {
            if (input.containsKey(neuronId)) {
                neur = map.get(neuronId);
                neur.setCyclingNeuronInputMapping(inputResolver.getCycleNeuronAddressMapping());
                neur.setSignalHistory(inputResolver.getSignalsHistoryStorage());
                neur.addSignals(input.get(neuronId));
                neur.setRun(inputResolver.getRun());
                neur.setCurrentLoop(inputResolver.getCurrentLoop());
                ns.addNeuron(neur);
            }
        }
        ns.process(threads);

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
        for (INeuron n : map.values()) {
            if (!n.getAxon().isConnectionsWrapped()) {
                n.getAxon().wrapConnections();
            }
            neurons.add(n);
        }
        layerMeta.saveNeurons(neurons);
        layerMeta.setLayerMetaParams(metaParams);
        layerMeta.dumpLayer();

    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getResults() {
        HashMap<Integer, HashMap<Long, List<ISignal>>> result = new HashMap<>();
        for (Long neurId : map.keySet()) {
            INeuron neur = map.get(neurId);
            IAxon axon = neur.getAxon();
            if (axon.isConnectionsWrapped()) {
                axon.unwrapConnections();
            }
            HashMap<ISignal, List<ISynapse>> tMap = axon.processSignals(neur.getResult());
            for (ISignal signal : tMap.keySet()) {
                signal.setSourceLayerId(this.layerId);
                signal.setSourceNeuronId(neurId);
                for (ISynapse connection : tMap.get(signal)) {
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
    public void sendCallBack(String name, List<ISignal> signals) {
        inputResolver.sendCallBack(name, signals);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Layer layer = (Layer) o;
        return layerId == layer.layerId &&
                Objects.equals(map, layer.map) &&
                Objects.equals(isProcessed, layer.isProcessed) &&
                Objects.equals(notProcessed, layer.notProcessed) &&
                Objects.equals(rules, layer.rules);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map, isProcessed, notProcessed, rules, layerId);
    }
}
