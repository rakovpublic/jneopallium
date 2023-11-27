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
import com.rakovpublic.jneuropallium.worker.model.ConfigurationRecord;
import com.rakovpublic.jneuropallium.worker.model.ConfigurationUpdateRequest;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ResultInterpreter;
import com.rakovpublic.jneuropallium.worker.net.layers.ResultLayerHolder;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.*;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;


public class ConfigurationServiceImpl implements ConfigurationService {
    private static final Logger logger = LogManager.getLogger(ConfigurationServiceImpl.class);
    private IInputService inputService = null;
    private ReconnectStrategy reconnectStrategy = null;
    private ResultInterpreter resultInterpreter = null;


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

        return resultInterpreter;
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
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for inputLoadingStrategy " + configuration.toString());
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
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for IResultLayerRunner " + configuration.toString());
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
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISignalsPersistStorage " + configuration.toString());
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
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISignalHistoryStorage " + configuration.toString());
        }

        try {
            layersMeta = (ILayersMeta) mapper.readValue(configuration.getLayersMetaJson(), Class.forName(configuration.getLayersMetaClass()));
            layersMeta.setRootPath(configuration.getLayersMetaPath());
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ILayersMeta for class " + configuration.getLayersMetaClass(), e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ILayersMeta " + configuration.toString());
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
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISplitInput " + configuration.toString());
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
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for reconnectStrategy " + configuration.toString());
        }
        try {
            if (configuration.getResultInterpreterJson() != null) {
                resultInterpreter = (ResultInterpreter) mapper.readValue(configuration.getResultInterpreterJson(), Class.forName(configuration.getResultInterpreterClass()));
            } else {
                resultInterpreter = (ResultInterpreter) Class.forName(configuration.getResultInterpreterClass()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of resultInterpreter for class " + configuration.getResultInterpreterClass(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of resultInterpreter for json " + configuration.getResultInterpreterJson(), e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for resultInterpreter " + configuration.toString());
        }
        List<String> discriminators = configuration.getDiscriminators();
        HashMap<String, IInputLoadingStrategy> discriminatorsLoadingStrategies = new HashMap<>();
        HashMap<String, ISignalsPersistStorage> discriminatorsSignalStorage = new HashMap<>();
        HashMap<String, ISignalHistoryStorage> discriminatorsSignalStorageHistory = new HashMap<>();
        HashMap<String, HashMap<IInitInput, InputStatusMeta>> inputDiscriminatorStatuses = new HashMap<>();
        ISplitInput discriminatorSplitInput = null;
        HashMap<String, ILayersMeta> discriminatorsLayers = new HashMap<>();
        ResultLayerHolder resultLayerHolder = new ResultLayerHolder();
        for (String name : discriminators) {
            IInputLoadingStrategy discriminatorRunningStrategy = null;
            ISignalsPersistStorage discriminatorSignalsPersist = null;
            ISignalHistoryStorage discriminatorSignalHistoryStorage = null;
            ILayersMeta discriminatorLayersMeta = null;
            HashMap<IInitInput, InputStatusMeta> inputs = new HashMap<>();
            try {
                if (configuration.getDiscriminatorsLoadingStrategies().get(name).getJson() != null) {
                    discriminatorRunningStrategy = (IInputLoadingStrategy) mapper.readValue(configuration.getDiscriminatorsLoadingStrategies().get(name).getJson(), Class.forName(configuration.getDiscriminatorsLoadingStrategies().get(name).getClassName()));
                } else {
                    discriminatorRunningStrategy = (IInputLoadingStrategy) Class.forName(configuration.getDiscriminatorsLoadingStrategies().get(name).getClassName()).newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Cannot create instance of inputLoadingStrategy for class " + configuration.getDiscriminatorsLoadingStrategies().get(name).getClassName(), e);
            } catch (JsonProcessingException e) {
                logger.error("Cannot create instance of inputLoadingStrategy for json " + configuration.getDiscriminatorsLoadingStrategies().get(name).getJson(), e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " + configuration.toString());
            }
            try {
                if (configuration.getDiscriminatorsSignalStorage().get(name).getJson() != null) {
                    discriminatorSignalsPersist = (ISignalsPersistStorage) mapper.readValue(configuration.getDiscriminatorsSignalStorage().get(name).getJson(), Class.forName(configuration.getDiscriminatorsSignalStorage().get(name).getClassName()));
                } else {
                    discriminatorSignalsPersist = (ISignalsPersistStorage) Class.forName(configuration.getDiscriminatorsSignalStorage().get(name).getClassName()).newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Cannot create instance of ISignalsPersistStorage for class " + configuration.getDiscriminatorsSignalStorage().get(name).getClassName(), e);
            } catch (JsonProcessingException e) {
                logger.error("Cannot create instance of ISignalsPersistStorage for json " + configuration.getDiscriminatorsSignalStorage().get(name).getJson(), e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " + configuration.toString());
            }
            try {
                if (configuration.getDiscriminatorsSignalStorageHistory().get(name).getJson() != null) {
                    discriminatorSignalHistoryStorage = (ISignalHistoryStorage) mapper.readValue(configuration.getDiscriminatorsSignalStorageHistory().get(name).getJson(), Class.forName(configuration.getDiscriminatorsSignalStorageHistory().get(name).getClassName()));
                } else {
                    discriminatorSignalHistoryStorage = (ISignalHistoryStorage) Class.forName(configuration.getDiscriminatorsSignalStorageHistory().get(name).getClassName()).newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Cannot create instance of ISignalHistoryStorage for class " + configuration.getDiscriminatorsSignalStorageHistory().get(name).getClassName(), e);
            } catch (JsonProcessingException e) {
                logger.error("Cannot create instance of ISignalHistoryStorage for json " + configuration.getDiscriminatorsSignalStorageHistory().get(name).getJson(), e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " + configuration.toString());
            }

            try {
                discriminatorLayersMeta = (ILayersMeta) mapper.readValue(configuration.getDiscriminatorsLayers().get(name).getJson(), Class.forName(configuration.getDiscriminatorsLayers().get(name).getClassName()));
                discriminatorLayersMeta.setRootPath(configuration.getLayersMetaPath());
            } catch (JsonProcessingException | ClassNotFoundException e) {
                logger.error("Cannot create instance of ILayersMeta for class " + configuration.getDiscriminatorsLayers().get(name).getClassName(), e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " + configuration.toString());
            }
            discriminatorsLoadingStrategies.put(name, discriminatorRunningStrategy);
            discriminatorsSignalStorage.put(name, discriminatorSignalsPersist);
            discriminatorsSignalStorageHistory.put(name, discriminatorSignalHistoryStorage);
            discriminatorsLayers.put(name, discriminatorLayersMeta);
            InMemoryInitInput inMemoryInitInput = new InMemoryInitInputImpl(name);
            InMemoryDiscriminatorResultSignals inMemoryDiscriminatorResultSignals = new InMemoryDiscriminatorResultSignals(inMemoryInitInput, name + "Result", resultLayerHolder, new ProcessingFrequency(1l, 1));
            InMemoryDiscriminatorSourceSignals inMemoryDiscriminatorSourceSignals = new InMemoryDiscriminatorSourceSignals(runningStrategy, 0l, 0, name + "Input", new ProcessingFrequency(1l, 1));
            inputs.put(inMemoryDiscriminatorResultSignals, new InputStatusMeta(true, true, inMemoryDiscriminatorResultSignals.getName()));
            inputs.put(inMemoryDiscriminatorSourceSignals, new InputStatusMeta(true, true, inMemoryDiscriminatorSourceSignals.getName()));
            runningStrategy.registerInput(inMemoryInitInput, getInputInitStrategy(configuration.getDiscriminatorsInitStrategyInputsCallback().get(name)));
            discriminatorRunningStrategy.registerInput(inMemoryDiscriminatorSourceSignals, getInputInitStrategy(configuration.getDiscriminatorsInitStrategySource().get(name)));
            discriminatorRunningStrategy.registerInput(inMemoryDiscriminatorResultSignals, getInputInitStrategy(configuration.getDiscriminatorsInitStrategyInputs().get(name)));
            inputDiscriminatorStatuses.put(name, inputs);
        }
        try {
            if (configuration.getDiscriminatorSplitInputJson() != null) {
                discriminatorSplitInput = (ISplitInput) mapper.readValue(configuration.getDiscriminatorSplitInputJson(), Class.forName(configuration.getDiscriminatorSplitInput()));
            } else {
                discriminatorSplitInput = (ISplitInput) Class.forName(configuration.getDiscriminatorSplitInput()).newInstance();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Cannot create instance of ISplitInput for class " + configuration.getDiscriminatorSplitInput(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISplitInput for json " + configuration.getDiscriminatorSplitInputJson(), e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISplitInput " + configuration.toString());
        }
        inputService = new InputService(signalsPersist, layersMeta, splitInput, partitions, runningStrategy, signalHistoryStorage, resultLayerRunner, discriminatorsLoadingStrategies, discriminatorsSignalStorage, discriminatorsSignalStorageHistory, inputDiscriminatorStatuses, discriminatorSplitInput, configuration.getNodeTimeout(), resultLayerHolder);
        inputService.updateDiscriminators(discriminatorsLayers);
    }

    private InputInitStrategy getInputInitStrategy(ConfigurationRecord json) {
        ObjectMapper mapper = new ObjectMapper();
        InputInitStrategy result = null;
        try {
            if (json.getJson() != null) {
                result = (InputInitStrategy) mapper.readValue(json.getJson(), Class.forName(json.getClassName()));
            } else {
                result = (InputInitStrategy) Class.forName(json.getClassName()).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ISignalHistoryStorage for class " + json.getClassName(), e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISignalHistoryStorage for json " + json.getJson(), e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for discriminator  config " + json.toString());
        }
        return result;
    }


}
