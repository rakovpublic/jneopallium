/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.model.ConfigurationRecord;
import com.rakovpublic.jneuropallium.worker.net.core.IInputService;
import com.rakovpublic.jneuropallium.worker.net.core.IResultLayerRunner;
import com.rakovpublic.jneuropallium.worker.net.core.InputService;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.ResultInterpreter;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.ResultLayerHolder;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.net.signals.*;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.IInitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.ISplitInput;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory.InMemoryDiscriminatorResultSignals;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory.InMemoryDiscriminatorSourceSignals;
import com.rakovpublic.jneuropallium.worker.net.signals.storage.inmemory.InMemoryInitInputImpl;
import com.rakovpublic.jneuropallium.worker.util.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class GRPCServerApplication implements IApplication {
    private static final Logger logger = LogManager.getLogger(GRPCServerApplication.class);
    private ReconnectStrategy reconnectStrategy =null;
    private ResultInterpreter resultInterpreter =null;

    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {
        Server server = ServerBuilder
                .forPort(8080)
                .addService(new GRPCServer(getInputService(context))).build();

        try {
            server.start();
            server.awaitTermination();
        } catch (IOException | InterruptedException e) {
            logger.error("Server cannot start reason: ",e);
        }

    }
    private IInputService getInputService(IContext context){

        IInputService inputService = null;
        ISignalsPersistStorage signalsPersist = null;
        ILayersMeta layersMeta = null;
        ISplitInput splitInput = null;
        Integer partitions = Integer.parseInt(context.getProperty("partitions"));
        IInputLoadingStrategy runningStrategy = null;
        ISignalHistoryStorage signalHistoryStorage = null;
        IResultLayerRunner resultLayerRunner = null;
        ObjectMapper mapper = new ObjectMapper();
        String inputLoadingStrategyJson = context.getProperty("inputLoadingStrategyJson");
        String inputLoadingStrategyClass = context.getProperty("inputLoadingStrategyClass");
            try {
            if (inputLoadingStrategyJson != null) {
                runningStrategy = (IInputLoadingStrategy) mapper.readValue(inputLoadingStrategyJson, Class.forName(inputLoadingStrategyClass));
            } else {
                runningStrategy = (IInputLoadingStrategy) Class.forName(inputLoadingStrategyClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of inputLoadingStrategy for class " +inputLoadingStrategyClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of inputLoadingStrategy for json " +inputLoadingStrategyJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for inputLoadingStrategy ");
        }
        String resultRunnerJson = context.getProperty("resultRunnerJson");
        String resultRunnerClass = context.getProperty("resultRunnerClass");
        try {
            if (resultRunnerJson != null) {
                resultLayerRunner = (IResultLayerRunner) mapper.readValue(resultRunnerJson, Class.forName(resultRunnerClass));
            } else {
                resultLayerRunner = (IResultLayerRunner) Class.forName(resultRunnerClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of IResultLayerRunner for class " + resultRunnerClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of IResultLayerRunner for json " + resultRunnerJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for IResultLayerRunner ");
        }
        String signalsPersistJson  = context.getProperty("signalsPersistJson");
        String signalsPersistClass  = context.getProperty("signalsPersistClass");
        try {
            if (signalsPersistJson != null) {
                signalsPersist = (ISignalsPersistStorage) mapper.readValue(signalsPersistJson, Class.forName(signalsPersistClass));
            } else {
                signalsPersist = (ISignalsPersistStorage) Class.forName(signalsPersistClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ISignalsPersistStorage for class " + signalsPersistClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISignalsPersistStorage for json " + signalsPersistJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISignalsPersistStorage ");
        }
        String historyJson  = context.getProperty("historyJson");
        String historyClass  = context.getProperty("historyClass");
        try {
            if (historyJson != null) {
                signalHistoryStorage = (ISignalHistoryStorage) mapper.readValue(historyJson, Class.forName(historyClass));
            } else {
                signalHistoryStorage = (ISignalHistoryStorage) Class.forName(historyClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ISignalHistoryStorage for class " + historyClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISignalHistoryStorage for json " + historyJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISignalHistoryStorage ");
        }
        String layersMetaJson  = context.getProperty("layersMetaJson");
        String layersMetaClass  = context.getProperty("layersMetaClass");
        try {
            layersMeta = (ILayersMeta) mapper.readValue(layersMetaJson, Class.forName(layersMetaClass));
            layersMeta.setRootPath(context.getProperty("rootPath"));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ILayersMeta for class " + layersMetaClass, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ILayersMeta ");
        }
        String splitInputJson  = context.getProperty("splitInputJson");
        String splitInputClass  = context.getProperty("splitInputClass");
        try {
            if (splitInputJson != null) {
                splitInput = (ISplitInput) mapper.readValue(splitInputJson, Class.forName(splitInputClass));
            } else {
                splitInput = (ISplitInput) Class.forName(splitInputClass).newInstance();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Cannot create instance of ISplitInput for class " + splitInputClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISplitInput for json " + splitInputJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISplitInput ");
        }
        String reconnectStrategyJson  = context.getProperty("reconnectStrategyJson");
        String reconnectStrategyClass  = context.getProperty("reconnectStrategyClass");
        try {
            if (reconnectStrategyJson != null) {
                reconnectStrategy = (ReconnectStrategy) mapper.readValue(reconnectStrategyJson, Class.forName(reconnectStrategyClass));
            } else {
                reconnectStrategy = (ReconnectStrategy) Class.forName(reconnectStrategyClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of ReconnectStrategy for class " +reconnectStrategyClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ReconnectStrategy for json " + reconnectStrategyJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for reconnectStrategy ");
        }
        String resultInterpreterJson  = context.getProperty("resultInterpreterJson");
        String resultInterpreterClass  = context.getProperty("resultInterpreterClass");
        try {
            if (resultInterpreterJson != null) {
                resultInterpreter = (ResultInterpreter) mapper.readValue(resultInterpreterJson, Class.forName(resultInterpreterClass));
            } else {
                resultInterpreter = (ResultInterpreter) Class.forName(resultInterpreterClass).newInstance();
            }
        } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
            logger.error("Cannot create instance of resultInterpreter for class " + resultInterpreterClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of resultInterpreter for json " +resultInterpreterJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for resultInterpreter " );
        }
        String discriminatorNames = context.getProperty("discriminatorNames");
        List<String> discriminators = new LinkedList<>();
        if(discriminatorNames!=  null){
            discriminators = Arrays.asList(discriminatorNames.split(","));
        }
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
            String discriminatorsLoadingStrategiesJson =context.getProperty(name+".discriminatorsLoadingStrategiesJson");
            String discriminatorsLoadingStrategiesClass =context.getProperty(name+".discriminatorsLoadingStrategiesClass");
            try {
                if (discriminatorsLoadingStrategiesJson!= null) {
                    discriminatorRunningStrategy = (IInputLoadingStrategy) mapper.readValue(discriminatorsLoadingStrategiesJson, Class.forName(discriminatorsLoadingStrategiesClass));
                } else {
                    discriminatorRunningStrategy = (IInputLoadingStrategy) Class.forName(discriminatorsLoadingStrategiesClass).newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Cannot create instance of inputLoadingStrategy for class " + discriminatorsLoadingStrategiesClass, e);
            } catch (JsonProcessingException e) {
                logger.error("Cannot create instance of inputLoadingStrategy for json " , e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " );
            }
            String discriminatorsSignalStorageJson =context.getProperty(name+".discriminatorsSignalStorageJson");
            String discriminatorsSignalStorageClass =context.getProperty(name+".discriminatorsSignalStorageClass");
            try {
                if (discriminatorsSignalStorageJson != null) {
                    discriminatorSignalsPersist = (ISignalsPersistStorage) mapper.readValue(discriminatorsSignalStorageJson, Class.forName(discriminatorsSignalStorageClass));
                } else {
                    discriminatorSignalsPersist = (ISignalsPersistStorage) Class.forName(discriminatorsSignalStorageClass).newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Cannot create instance of ISignalsPersistStorage for class " + discriminatorsSignalStorageClass, e);
            } catch (JsonProcessingException e) {
                logger.error("Cannot create instance of ISignalsPersistStorage for json " + discriminatorsSignalStorageJson, e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " );
            }
            String discriminatorsSignalStorageHistoryJson =context.getProperty(name+".discriminatorsSignalStorageHistoryJson");
            String discriminatorsSignalStorageHistoryClass =context.getProperty(name+".discriminatorsSignalStorageHistoryClass");
            try {
                if (discriminatorsSignalStorageHistoryJson != null) {
                    discriminatorSignalHistoryStorage = (ISignalHistoryStorage) mapper.readValue(discriminatorsSignalStorageHistoryJson, Class.forName(discriminatorsSignalStorageHistoryClass));
                } else {
                    discriminatorSignalHistoryStorage = (ISignalHistoryStorage) Class.forName(discriminatorsSignalStorageHistoryClass).newInstance();
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException e) {
                logger.error("Cannot create instance of ISignalHistoryStorage for class " + discriminatorsSignalStorageHistoryClass, e);
            } catch (JsonProcessingException e) {
                logger.error("Cannot create instance of ISignalHistoryStorage for json " + discriminatorsSignalStorageHistoryJson, e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config "+discriminatorsSignalStorageHistoryJson);
            }

            String discriminatorsLayersJson =context.getProperty(name+".discriminatorsLayersJson");
            String discriminatorsLayersClass =context.getProperty(name+".discriminatorsLayersClass");
            try {
                discriminatorLayersMeta = (ILayersMeta) mapper.readValue(discriminatorsLayersJson, Class.forName(discriminatorsLayersClass));
                discriminatorLayersMeta.setRootPath(context.getProperty(name+".layersMetaPath"));
            } catch (JsonProcessingException | ClassNotFoundException e) {
                logger.error("Cannot create instance of ILayersMeta for class " + discriminatorsLayersClass, e);
            } catch (NullPointerException e) {
                logger.error("Wrong configuration for discriminator " + name + " config " );
            }
            discriminatorsLoadingStrategies.put(name, discriminatorRunningStrategy);
            discriminatorsSignalStorage.put(name, discriminatorSignalsPersist);
            discriminatorsSignalStorageHistory.put(name, discriminatorSignalHistoryStorage);
            discriminatorsLayers.put(name, discriminatorLayersMeta);
            InMemoryInitInput inMemoryInitInput = new InMemoryInitInputImpl(name, new ProcessingFrequency(1l, 1));
            InMemoryDiscriminatorResultSignals inMemoryDiscriminatorResultSignals = new InMemoryDiscriminatorResultSignals(inMemoryInitInput, name + "Result", resultLayerHolder, new ProcessingFrequency(1l, 1));
            InMemoryDiscriminatorSourceSignals inMemoryDiscriminatorSourceSignals = new InMemoryDiscriminatorSourceSignals(runningStrategy, 0l, 0, name + "Input", new ProcessingFrequency(1l, 1));
            inputs.put(inMemoryDiscriminatorResultSignals, new InputStatusMeta(true, true, inMemoryDiscriminatorResultSignals.getName()));
            inputs.put(inMemoryDiscriminatorSourceSignals, new InputStatusMeta(true, true, inMemoryDiscriminatorSourceSignals.getName()));
            runningStrategy.registerInput(inMemoryInitInput, getInputInitStrategy(new ConfigurationRecord(context.getProperty(name+".discriminatorsInitStrategyInputsCallbackClass"),context.getProperty(name+".discriminatorsInitStrategyInputsCallbackJson"))));
            discriminatorRunningStrategy.registerInput(inMemoryDiscriminatorSourceSignals, getInputInitStrategy(new ConfigurationRecord(context.getProperty(name+".discriminatorsInitStrategySourceClass"),context.getProperty(name+".discriminatorsInitStrategySourceJson"))));
            discriminatorRunningStrategy.registerInput(inMemoryDiscriminatorResultSignals, getInputInitStrategy(new ConfigurationRecord(context.getProperty(name+".discriminatorsInitStrategyInputsClass"),context.getProperty(name+".discriminatorsInitStrategyInputsJson"))));
            inputDiscriminatorStatuses.put(name, inputs);
        }
        String discriminatorSplitInputJson =context.getProperty("discriminatorSplitInputJson");
        String discriminatorSplitInputClass =context.getProperty("discriminatorSplitInputClass");
        try {
            if (discriminatorSplitInputJson != null) {
                discriminatorSplitInput = (ISplitInput) mapper.readValue(discriminatorSplitInputJson, Class.forName(discriminatorSplitInputClass));
            } else {
                discriminatorSplitInput = (ISplitInput) Class.forName(discriminatorSplitInputClass).newInstance();
            }
        } catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
            logger.error("Cannot create instance of ISplitInput for class " +discriminatorSplitInputClass, e);
        } catch (JsonProcessingException e) {
            logger.error("Cannot create instance of ISplitInput for json " + discriminatorSplitInputJson, e);
        } catch (NullPointerException e) {
            logger.error("Wrong configuration for ISplitInput ");
        }
        inputService = new InputService(signalsPersist, layersMeta, splitInput, partitions, runningStrategy, signalHistoryStorage, resultLayerRunner, discriminatorsLoadingStrategies, discriminatorsSignalStorage, discriminatorsSignalStorageHistory, inputDiscriminatorStatuses, discriminatorSplitInput,Long.parseLong( context.getProperty("nodeTimeout")), resultLayerHolder);
        inputService.updateDiscriminators(discriminatorsLayers);
        return inputService;
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
