/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.MasterServiceGrpc;
import com.rakovpublic.jneuropallium.worker.net.core.IInputService;
import com.rakovpublic.jneuropallium.worker.net.core.InputService;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import com.rakovpublic.jneuropallium.worker.util.IContext;

import java.util.HashMap;
import java.util.List;


public class GRPCServer extends MasterServiceGrpc.MasterServiceImplBase {
    private IInputService iInputService;
    private IContext context;

    public GRPCServer(IInputService iInputService, IContext context) {
        this.iInputService = iInputService;
        this.context = context;
    }

    @Override
    public void save(com.rakovpublic.jneuropallium.worker.Result request,
                     io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        HashMap<Integer,HashMap<Long, List<ISignal>>> result = new HashMap<>();
        for(Integer layerId: request.getResultMap().keySet()){

        }
        iInputService.uploadWorkerResult(request.getNodeIdentifier(),result);


    }

    /**
     */
    @Override
    public void saveDiscriminator(com.rakovpublic.jneuropallium.worker.ResultDiscriminator request,
                                  io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
        HashMap<Integer,HashMap<Long, List<ISignal>>> result = new HashMap<>();
        for(Integer layerId: request.getResultMap().keySet()){

        }
        iInputService.uploadDiscriminatorWorkerResult(request.getNodeIdentifier(),request.getDiscriminatorName(),result);
    }

    /**
     */
    @Override
    public void getRun(com.rakovpublic.jneuropallium.worker.NodeRequest request,
                       io.grpc.stub.StreamObserver<com.rakovpublic.jneuropallium.worker.SplitInputConfig> responseObserver) {
        ISplitInput  payload = iInputService.getNext(request.getNodeIdentifier());
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
                        e.printStackTrace();
                    }
                }
            }
        }


    }

}
