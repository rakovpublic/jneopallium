/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.MasterServiceGrpc;
import com.rakovpublic.jneuropallium.worker.SplitInputConfig;
import com.rakovpublic.jneuropallium.worker.net.core.IInputService;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;


public class GRPCServer extends MasterServiceGrpc.MasterServiceImplBase {
    private static final Logger logger = LogManager.getLogger(GRPCServer.class);
    private final IInputService iInputService;

    public GRPCServer(IInputService iInputService) {
        this.iInputService = iInputService;
    }

    @Override
    public void save(com.rakovpublic.jneuropallium.worker.Result request,
                     io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new HashMap<>();
        for (Integer layerId : request.getResultMap().keySet()) {
            parseAndSave(request.getResultMap().get(layerId), result, layerId);
        }
        iInputService.uploadWorkerResult(request.getNodeIdentifier(), result);


    }

    /**
     *
     */
    @Override
    public void saveDiscriminator(com.rakovpublic.jneuropallium.worker.ResultDiscriminator request,
                                  io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result = new HashMap<>();
        for (Integer layerId : request.getResultMap().keySet()) {
            parseAndSave(request.getResultMap().get(layerId), result, layerId);
        }
        iInputService.uploadDiscriminatorWorkerResult(request.getNodeIdentifier(), request.getDiscriminatorName(), result);
    }

    /**
     *
     */
    @Override
    public void getRun(com.rakovpublic.jneuropallium.worker.NodeRequest request,
                       io.grpc.stub.StreamObserver<com.rakovpublic.jneuropallium.worker.SplitInputConfig> responseObserver) {
        ISplitInput payload = iInputService.getNext(request.getNodeIdentifier());
        if (payload == null) {
            if (iInputService.hasDiscriminators() && !iInputService.isDiscriminatorsDone() && iInputService.runCompleted()) {
                iInputService.prepareDiscriminatorsInputs();
                payload = iInputService.getNextDiscriminators(request.getNodeIdentifier());
            } else {
                if (iInputService.isResultValid() && iInputService.runCompleted()) {
                    iInputService.prepareResults();
                } else if (iInputService.runCompleted()) {
                    iInputService.nextRun();
                    iInputService.prepareInputs();
                    iInputService.nextRunDiscriminator();
                    payload = iInputService.getNext(request.getNodeIdentifier());
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        logger.error(e);
                    }
                }
            }
        }
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            SplitInputConfig splitInputConfig = SplitInputConfig.newBuilder().setDiscriminatorName(payload.getDiscriminatorName())
                    .setEnd(payload.getEnd()).setInputResolverClass(payload.getInputResolver().getClass().getCanonicalName())
                    .setInputResolverJson(objectMapper.writeValueAsString(payload.getInputResolver()))
                    .setLayerId(payload.getLayerId()).setLayersMetaClass(iInputService.getLayersMeta().getClass().getCanonicalName())
                    .setLayersMetaJson(objectMapper.writeValueAsString(iInputService.getLayersMeta()))
                    .setNodeId(request.getNodeIdentifier()).setStart(payload.getStart()).setThreads(payload.getThreads()).build();
            responseObserver.onNext(splitInputConfig);
            responseObserver.onCompleted();
        } catch (JsonProcessingException e) {
            logger.error(e);
            responseObserver.onError(e);
        }


    }

    private void parseAndSave(String input, HashMap<Integer, HashMap<Long, CopyOnWriteArrayList<ISignal>>> result, Integer layerId) {
        JsonElement jelement = new JsonParser().parse(input);
        HashMap<Long, CopyOnWriteArrayList<ISignal>> signalsMap = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        for (Map.Entry<String, JsonElement> e : jelement.getAsJsonObject().entrySet()) {
            Long neuronId = Long.parseLong(e.getKey());
            CopyOnWriteArrayList<ISignal> signals = new CopyOnWriteArrayList<>();
            for (JsonElement signal : e.getValue().getAsJsonArray()) {
                String cc = signal.getAsJsonObject().getAsJsonPrimitive("currentClassName").getAsString();
                try {
                    signals.add((ISignal) mapper.readValue(signal.getAsJsonObject().toString(), Class.forName(cc)));
                } catch (JsonProcessingException ex) {
                    logger.error("Cannot parse this signal json " + signal.getAsJsonObject().toString(), ex);
                } catch (ClassNotFoundException ex) {
                    logger.error("Cannot find this signal class class " + cc, ex);
                }
            }
            signalsMap.put(neuronId, signals);
        }
        result.put(layerId, signalsMap);
    }

}
