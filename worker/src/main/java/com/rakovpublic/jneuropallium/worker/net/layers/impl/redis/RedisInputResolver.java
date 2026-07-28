/*
 * Copyright (c) 2026. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl.redis;

import com.rakovpublic.jneuropallium.worker.net.signals.IInputSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignalsPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.InputInitStrategy;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisSignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.redis.RedisSignalStorage;

import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

/**
 * Worker side view of the run state.
 * <p>
 * The in-memory resolver keeps the input loading strategy - and with it the whole input
 * history - as a field, which would be serialised into every partition assignment. This one
 * holds only what a worker actually needs: where Redis is, and which epoch/loop the current
 * partition belongs to. Populating input stays the master's job.
 */
public class RedisInputResolver implements IInputResolver {
    private final String host;
    private final Integer port;
    private final String neuronNetName;
    private final Long run;
    private final Integer loop;
    private final HashMap<String, Long> cycleNeuronMapping;
    private final ISignalsPersistStorage signalsPersistStorage;
    private final ISignalHistoryStorage signalHistoryStorage;

    public RedisInputResolver(String host, Integer port, String neuronNetName, Long run, Integer loop,
                              HashMap<String, Long> cycleNeuronMapping) {
        this.host = host;
        this.port = port;
        this.neuronNetName = neuronNetName;
        this.run = run == null ? 0L : run;
        this.loop = loop == null ? 0 : loop;
        this.cycleNeuronMapping = cycleNeuronMapping == null ? new HashMap<>() : cycleNeuronMapping;
        this.signalsPersistStorage = new RedisSignalStorage(host, port, neuronNetName);
        this.signalHistoryStorage = new RedisSignalHistoryStorage(host, port, neuronNetName);
    }

    @Override
    public void registerInput(IInitInput iInputSource, boolean isMandatory, InputInitStrategy initStrategy) {
        throw new UnsupportedOperationException("Inputs are registered on the master, not on a worker");
    }

    @Override
    public ISignalsPersistStorage getSignalPersistStorage() {
        return signalsPersistStorage;
    }

    @Override
    public ISignalHistoryStorage getSignalsHistoryStorage() {
        return signalHistoryStorage;
    }

    @Override
    public TreeMap<Long, TreeMap<Integer, List<IInputSignal>>> getInputHistory() {
        return new TreeMap<>();
    }

    @Override
    public HashMap<String, Long> getCycleNeuronAddressMapping() {
        return cycleNeuronMapping;
    }

    @Override
    public Integer getCurrentLoop() {
        return loop;
    }

    @Override
    public Long getRun() {
        return run;
    }

    @Override
    public void saveHistory() {
        signalHistoryStorage.save(signalsPersistStorage.getAllSignals(), run, loop);
    }

    @Override
    public void populateInput() {
        throw new UnsupportedOperationException("Input is populated by the master");
    }

    @Override
    public HashMap<String, List<IResultSignal>> getDesiredResult() {
        return new HashMap<>();
    }

    @Override
    public void sendCallBack(String name, List<ISignal> signals) {
        // The worker reports completion over HTTP; signal callbacks are a master side concern.
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getNeuronNetName() {
        return neuronNetName;
    }
}
