/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.rakovpublic.jneuropallium.master.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


//TODO: add implementation
public class ConfigurationServiceImpl implements ConfigurationService {
    private static final Logger logger = LogManager.getLogger(ConfigurationServiceImpl.class);
    private IInputService inputService;




    @Override
    public void update(ConfigurationUpdateRequest request) {

        updateInputService(request);
    }

    @Override
    public IInputService getInputService() {
        return inputService;
    }

    @Override
    public ReconnectStrategy getReconnectionStrategy() {
        return null;
    }

    private void updateInputService(ConfigurationUpdateRequest configuration){
        ISignalsPersistStorage signalsPersist;
        ILayersMeta layersMeta;
        ISplitInput splitInput;
        Integer partitions = configuration.getPartitions();
        IInputLoadingStrategy runningStrategy = null;
        try {
             runningStrategy = (IInputLoadingStrategy)Class.forName(configuration.getInputLoadingStrategyClass()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.error("Cannot create instance of inputLoadingStrategy for class " + configuration.getInputLoadingStrategyClass(),e);
        }
        ISignalHistoryStorage signalHistoryStorage;
        IResultLayerRunner resultLayerRunner = null;
        try {
            resultLayerRunner = (IResultLayerRunner) Class.forName(configuration.getResultRunnerClass()).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            logger.error("Cannot create instance of IResultLayerRunner for class " + configuration.getResultRunnerClass(),e);
        }

    }

}
