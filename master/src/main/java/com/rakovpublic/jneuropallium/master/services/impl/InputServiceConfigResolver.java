/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.net.storages.*;

public class InputServiceConfigResolver {
    public static IInputService initService(){
        IInputService inputService = null;
        return parseServiceFields(inputService);
    }
    public static void updateService(IInputService inputService){
        parseServiceFields(inputService);

    }

    private static IInputService parseServiceFields(IInputService inputService ){
        ISignalsPersistStorage signalsPersist = null;
        ILayersMeta layersMeta= null;
        ISplitInput splitInput = null;
        Integer partitions = null;
        IInputLoadingStrategy runningStrategy= null;
        ISignalHistoryStorage signalHistoryStorage= null;
        IResultLayerRunner resultLayerRunner= null;
        //TODO: add config parsing
        if(inputService == null){
            return new InputService(signalsPersist,layersMeta,splitInput,partitions,runningStrategy,signalHistoryStorage,resultLayerRunner);
        }
        else {
            inputService.updateConfiguration(signalsPersist,layersMeta,splitInput,partitions,runningStrategy,signalHistoryStorage,resultLayerRunner);
            return inputService;
        }

    }
}
