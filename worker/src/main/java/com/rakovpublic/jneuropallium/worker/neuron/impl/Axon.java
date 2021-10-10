package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.neuron.ISynapse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Axon implements IAxon {
    private HashMap<Class<? extends ISignal>, List<ISynapse>> connectionMap;
    private HashMap<Integer, HashMap<Long, List<ISynapse>>> addressMap;
    private Boolean connectionsWrapped;


    public Axon() {
        this.connectionMap = new HashMap<>();
        this.addressMap = new HashMap<>();
        connectionsWrapped = false;
    }

    public Boolean isConnectionsWrapped() {
        return connectionsWrapped;
    }


    @Override
    public void resetConnection(HashMap<Class<? extends ISignal>, List<ISynapse>> newConnection) {
        connectionMap = newConnection;
    }

    @Override
    public <S extends ISignal> void putConnection(Class<S> cl, ISynapse<S> connection) {
        if (connectionMap.containsKey(cl)) {
            connectionMap.get(cl).add(connection);
        } else {
            List<ISynapse> tlist = new ArrayList<>();
            tlist.add(connection);
            connectionMap.put(cl, tlist);
        }
        if (addressMap.containsKey(connection.getTargetLayerId())) {
            HashMap<Long, List<ISynapse>> tMap = addressMap.get(connection.getTargetLayerId());
            if (tMap.containsKey(connection.getTargetNeuronId())) {
                tMap.get(connection.getTargetNeuronId()).add(connection);
            } else {
                List<ISynapse> tlist = new ArrayList<>();
                tlist.add(connection);
                tMap.put(connection.getTargetNeuronId(), tlist);
            }
        } else {
            HashMap<Long, List<ISynapse>> tMap = new HashMap<>();
            List<ISynapse> tlist = new ArrayList<>();
            tlist.add(connection);
            tMap.put(connection.getTargetNeuronId(), tlist);
            addressMap.put(connection.getTargetLayerId(), tMap);
        }
    }


    @Override
    public void cleanConnections() {
        connectionMap.clear();


    }

    @Override
    public HashMap<ISignal, List<ISynapse>> processSignals(List<ISignal> signal) {
        HashMap<ISignal, List<ISynapse>> result = new HashMap<>();
        for (ISignal s : signal) {
            Class<? extends ISignal> cl = s.getCurrentSignalClass();
            if (connectionMap.containsKey(cl)) {
                for (ISynapse con : connectionMap.get(cl)) {
                    ISignal resSignal = con.getWeight().process(s);
                    if (result.containsKey(resSignal)) {
                        result.get(resSignal).add(con);
                    } else {
                        List<ISynapse> cons = new ArrayList<>();
                        cons.add(con);
                        result.put(resSignal, cons);
                    }
                }
            } else {
                //TODO:add warn for logger;

            }
        }
        return result;
    }


    @Override
    public String toJSON() {
        return null;
    }

    @Override
    public HashMap<Integer, HashMap<Long, List<ISignal>>> getSignalResultStructure(HashMap<ISignal, List<ISynapse>> signalConnectionMap) {
        return null;
    }

    @Override
    public void destroyConnection(int layerId, Long neuronId, Class<? extends ISignal> clazz) {
        if (addressMap.containsKey(layerId) && addressMap.get(layerId).containsKey(neuronId)) {
            List<ISynapse> conns = addressMap.get(layerId).get(neuronId);
            for (ISynapse c : conns) {
                if (c.getWeight().getSignalClass().equals(clazz)) {
                    connectionMap.get(clazz).remove(c);
                    conns.remove(c);
                    break;
                }
            }
        }
    }

    @Override
    public void changeAllWeightsForNeuron(int layerId, Long neuronId, ISignal signal) {
        if (addressMap.containsKey(layerId) && addressMap.get(layerId).containsKey(neuronId)) {
            List<ISynapse> conns = addressMap.get(layerId).get(neuronId);
            for (ISynapse c : conns) {
                c.getWeight().changeWeight(signal);
            }
        }
    }

    @Override
    public void changeAllWeightsForNeuronAndSignal(int layerId, Long neuronId, Class<? extends ISignal> clazz, ISignal signal) {
        if (addressMap.containsKey(layerId) && addressMap.get(layerId).containsKey(neuronId)) {
            List<ISynapse> conns = addressMap.get(layerId).get(neuronId);
            for (ISynapse c : conns) {
                if (c.getWeight().getSignalClass().equals(clazz)) {
                    c.getWeight().changeWeight(signal);
                }
            }
        }
    }

    @Override
    public void changeAllWeights(ISignal signal) {
        for (Class<? extends ISignal> cl : connectionMap.keySet()) {
            for (ISynapse con : connectionMap.get(cl)) {
                con.getWeight().changeWeight(signal);
            }
        }

    }

    @Override
    public HashMap<Class<? extends ISignal>, List<ISynapse>> getConnectionMap() {
        return connectionMap;
    }

    @Override
    public void wrapConnections() {
        for (List<ISynapse> connections : connectionMap.values()) {
            for (ISynapse connection : connections) {
                connection.setWeight(new WeightWrapper(connection.getWeight()));
            }
        }
        for (HashMap<Long, List<ISynapse>> map : addressMap.values()) {
            for (List<ISynapse> connections : map.values()) {
                for (ISynapse connection : connections) {
                    connection.setWeight(new WeightWrapper(connection.getWeight()));
                }
            }
        }
        connectionsWrapped = true;
    }

    @Override
    public void unwrapConnections() {
        for (List<ISynapse> connections : connectionMap.values()) {
            for (ISynapse connection : connections) {
                connection.setWeight(((WeightWrapper) connection.getWeight()).getWeight());
            }
        }
        for (HashMap<Long, List<ISynapse>> map : addressMap.values()) {
            for (List<ISynapse> connections : map.values()) {
                for (ISynapse connection : connections) {
                    connection.setWeight(((WeightWrapper) connection.getWeight()).getWeight());
                }
            }
        }
        connectionsWrapped = false;
    }
}
