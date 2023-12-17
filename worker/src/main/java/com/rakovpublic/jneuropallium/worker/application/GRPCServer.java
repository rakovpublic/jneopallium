/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.MasterServiceGrpc;


public class GRPCServer extends MasterServiceGrpc.MasterServiceImplBase {
    @Override
    public void save(com.rakovpublic.jneuropallium.worker.Result request,
                     io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {

    }

    /**
     */
    @Override
    public void saveDiscriminator(com.rakovpublic.jneuropallium.worker.ResultDiscriminator request,
                                  io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {

    }

    /**
     */
    @Override
    public void getRun(com.rakovpublic.jneuropallium.worker.NodeRequest request,
                       io.grpc.stub.StreamObserver<com.rakovpublic.jneuropallium.worker.SplitInputConfig> responseObserver) {

    }

}
