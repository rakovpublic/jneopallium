/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.master.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.master.services.ConfigurationService;
import com.rakovpublic.jneuropallium.master.services.IInputService;
import com.rakovpublic.jneuropallium.master.services.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.exceptions.InputServiceInitException;
import com.rakovpublic.jneuropallium.worker.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.worker.net.DiscriminatorSplitInput;
import com.rakovpublic.jneuropallium.worker.net.layers.ResultInterpreter;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;


public class ConfigurationServiceImpl implements ConfigurationService {
    private static final Logger logger = LogManager.getLogger(ConfigurationServiceImpl.class);
    private IInputService inputService = null;
    private ReconnectStrategy reconnectStrategy = null;


    @Override
    public void update(ConfigurationUpdateRequest request) {

        updateInputService(request);
    }

    @Override
    public IInputService getInputService() {
        if (inputService == null) {
            throw new InputServiceInitException("Input service has not been registered");
        }

        return inputService;
    }

    @Override
    public ReconnectStrategy getReconnectionStrategy() {
        return reconnectStrategy;
    }

    @Override
    public ResultInterpreter getResultInterpreter() {
        //TODO: add parsing
        return null;
    }

    private void updateInputService(ConfigurationUpdateRequest configuration) {
        ISignalsPersistStorage signalsPersist = null;
        ILayersMeta layersMeta = null;
        ISplitInput splitInput = null;
        Integer partitions = configuration.getPartitions();
        IInputLoadingStrategy runningStrategy = null;
        ISignalHistoryStorage signalHistoryStorage = null;
        IResultLayerRunner resultLayerRunner = null;
        ObjectMapper mapper = new ObjectMapper();
        try {
            if (configuration.getInputLoadingStrategyJson() != null) {
                runningStrategy = (IInputLoadingStrategy) mapper.readValue(configuration.getInputLoadingStrategyJson(), Class.forName(configuration.getInputLoadingStrategyClass()));
            } else {
                runningStrategy = (IInputLoadingStrategy) Class.forName(configuration.getInputLoadingStrategyClass()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of inputLoadingStrategy for class " + configuration.getInputLoadingStrategyClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of inputLoadingStrategy for json " + configuration.getInputLoadingStrategyJson(), e);
        }

        try {
            if (configuration.getResultRunnerJson() != null) {
                resultLayerRunner = (IResultLayerRunner) mapper.readValue(configuration.getResultRunnerJson(), Class.forName(configuration.getResultRunnerClass()));
            } else {
                resultLayerRunner = (IResultLayerRunner) Class.forName(configuration.getResultRunnerClass()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of IResultLayerRunner for class " + configuration.getResultRunnerClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of IResultLayerRunner for json " + configuration.getResultRunnerJson(), e);
        }
        try {
            if (configuration.getSignalsPersistJson() != null) {
                signalsPersist = (ISignalsPersistStorage) mapper.readValue(configuration.getSignalsPersistJson(), Class.forName(configuration.getSignalsPersistClass()));
            } else {
                signalsPersist = (ISignalsPersistStorage) Class.forName(configuration.getSignalsPersistClass()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ISignalsPersistStorage for class " + configuration.getSignalsPersistClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISignalsPersistStorage for json " + configuration.getSignalsPersistJson(), e);
        }
        try {
            if (configuration.getHistoryJson() != null) {
                signalHistoryStorage = (ISignalHistoryStorage) mapper.readValue(configuration.getHistoryJson(), Class.forName(configuration.getHistoryClass()));
            } else {
                signalHistoryStorage = (ISignalHistoryStorage) Class.forName(configuration.getHistoryClass()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ISignalHistoryStorage for class " + configuration.getHistoryClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISignalHistoryStorage for json " + configuration.getHistoryJson(), e);
        }

        try {
            layersMeta = (ILayersMeta) mapper.readValue(configuration.getLayersMetaJson(), Class.forName(configuration.getLayersMetaClass()));
            layersMeta.setRootPath(configuration.getLayersMetaPath());
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ILayersMeta for class " + configuration.getLayersMetaClass(), e);
        }
        try {
            if (configuration.getSplitInputJson() != null) {
                splitInput = (ISplitInput) mapper.readValue(configuration.getSplitInputJson(), Class.forName(configuration.getSplitInputClass()));
            } else {
                splitInput = (ISplitInput) Class.forName(configuration.getSplitInputClass()).newInstance();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Cannot create instance of ISplitInput for class " + configuration.getSplitInputClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISplitInput for json " + configuration.getSplitInputJson(), e);
        }
        try {
            if (configuration.getReconnectStrategyJson() != null) {
                reconnectStrategy = (ReconnectStrategy) mapper.readValue(configuration.getReconnectStrategyJson(), Class.forName(configuration.getReconnectStrategyClass()));
            } else {
                reconnectStrategy = (ReconnectStrategy) Class.forName(configuration.getReconnectStrategyClass()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ReconnectStrategy for class " + configuration.getReconnectStrategyClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ReconnectStrategy for json " + configuration.getReconnectStrategyJson(), e);
        }
        //TODO: add parsing discriminators
        List<String> discriminators = configuration.getDiscriminators();
        HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies = null;
        HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage = null;
        HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory = null;
        HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses = null;
        DiscriminatorSplitInput discriminatorSplitInput = null;
        HashMap<String, ILayersMeta> discriminatorsLayers;
        for (String name : discriminators) {

        }


        inputService = new InputService(signalsPersist, layersMeta, splitInput, partitions, runningStrategy, signalHistoryStorage, resultLayerRunner, discriminatorsLoadingStrategies, discriminatorsSignalStorage, discriminatorsSignalStorageHistory, inputDiscriminatorStatuses, discriminatorSplitInput, configuration.getNodeTimeout());


    }

}
