/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.*;
import com.rakovpublic.jneuropallium.worker.exceptions.HttpClusterCommunicationException;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.neuron.IAxon;
import com.rakovpublic.jneuropallium.worker.net.neuron.INeuron;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.NeuronRunnerService;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.http.GRPCSplitInput;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GRPCClient implements IApplication {
    private static final Logger logger = LogManager.getLogger(HttpClusterApplication.class);
    private final static String UUID = java.util.UUID.randomUUID().toString();

    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(context.getProperty("masterHost"), 8080)
                .usePlaintext()
                .build();
        NodeRequest nodeRequest = NodeRequest.newBuilder().setNodeIdentifier(UUID).build();

        MasterServiceGrpc.MasterServiceBlockingStub stub
                = MasterServiceGrpc.newBlockingStub(channel);

        SplitInputConfig jsonSplitInput;
        while (true) {
            try {
                while ((jsonSplitInput = stub.getRun(nodeRequest)) == null) {
                    Thread.sleep(1000);
                }
                ISplitInput splitInput = parseSplitInput(jsonSplitInput);
                IInputResolver inputResolver = splitInput.getInputResolver();
                HashMap<Long, CopyOnWriteArrayList<ISignal>> input = inputResolver.getSignalPersistStorage().getLayerSignals(splitInput.getLayerId());
                NeuronRunnerService neuronRunnerService = NeuronRunnerService.getService();
                List<INeuron> neurons = (List<INeuron>) splitInput.getNeurons();


                for (INeuron neuron : neurons) {
                    neuron.setCurrentLoop(inputResolver.getCurrentLoop());
                    neuron.setRun(inputResolver.getRun());
                    neuron.addSignals(input.get(neuron.getId()));
                    neuron.setCyclingNeuronInputMapping(inputResolver.getCycleNeuronAddressMapping());
                    neuronRunnerService.addNeuron(neuron);
                }
                neuronRunnerService.process(splitInput.getThreads());
                HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new HashMap<>();
                while (neurons.size() > 0) {
                    for (INeuron neuron : neurons) {
                        if (neuron.hasResult()) {
                            IAxon axon = neuron.getAxon();
                            result.putAll(axon.getSignalResultStructure(axon.processSignals(neuron.getResult())));
                            splitInput.saveResults(result);
                            splitInput.saveNeuron(neuron);
                            neurons.remove(neuron);
                        }
                    }
                    //fault tolerance
                    if (neuronRunnerService.getNeuronQueue().isEmpty()) {
                        for (INeuron neuron : neurons) {
                            if (neuron.hasResult()) {
                                IAxon axon = neuron.getAxon();
                                result.putAll(axon.getSignalResultStructure(axon.processSignals(neuron.getResult())));
                                neurons.remove(neuron);
                            }
                        }
                        neuronRunnerService.getNeuronQueue().addAll(neurons);
                        neuronRunnerService.process(splitInput.getThreads());
                    }
                }
                HashMap<Integer, String> finalResult = new HashMap<>();
                ObjectMapper mapper = new ObjectMapper();
                for (Integer layerId : result.keySet()) {
                    try {
                        finalResult.put(layerId, mapper.writeValueAsString(result.get(layerId)));
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                }

                if (splitInput.getDiscriminatorName() != null) {
                    ResultDiscriminator resultDiscriminator = ResultDiscriminator.newBuilder().setNodeIdentifier(UUID).setDiscriminatorName(splitInput.getDiscriminatorName()).putAllResult(finalResult).build();
                    stub.saveDiscriminator(resultDiscriminator);
                } else {
                    Result resultResp = Result.newBuilder().setNodeIdentifier(UUID).putAllResult(finalResult).build();
                    stub.save(resultResp);
                }
            } catch (InterruptedException e) {
                logger.error("Cannot register node", e);
                throw new HttpClusterCommunicationException(e.getMessage());
            }
            channel.shutdown();
        }
    }

    private ISplitInput parseSplitInput(SplitInputConfig json) {
        ObjectMapper mapper = new ObjectMapper();
        IInputResolver inputResolver = null;
        ILayersMeta layersMeta = null;
        JsonElement inputResolverJson = new JsonParser().parse(json.getInputResolverJson());
        JsonElement layersMetaJson = new JsonParser().parse(json.getLayersMetaJson());
        try {
            inputResolver = (IInputResolver) mapper.readValue(inputResolverJson.getAsJsonObject().getAsString(), Class.forName(json.getInputResolverClass()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse input resolver " + inputResolverJson, e);
        }
        try {
            layersMeta = (ILayersMeta) mapper.readValue(layersMetaJson.getAsJsonObject().getAsString(), Class.forName(json.getLayersMetaClass()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse layers meta " + layersMetaJson, e);
        }
        ISplitInput result = new GRPCSplitInput(json.getNodeId(), inputResolver, json.getStart(), json.getEnd(), layersMeta, json.getDiscriminatorName(), json.getLayerId(), json.getThreads());
        return result;

    }
}
