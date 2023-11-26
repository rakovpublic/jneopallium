package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rakovpublic.jneuropallium.worker.net.layers.*;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.*;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.*;
import com.rakovpublic.jneuropallium.worker.net.storages.file.FileLayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.InMemoryDiscriminatorResultSignals;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.InMemoryDiscriminatorSourceSignals;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.InMemoryInitInputImpl;
import com.rakovpublic.jneuropallium.worker.net.storages.inmemory.ResultLayerHolder;
import com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory.InMemorySignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory.InMemorySignalPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructBuilder;
import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructMeta;
import com.rakovpublic.jneuropallium.worker.net.study.IDirectLearningAlgorithm;
import com.rakovpublic.jneuropallium.worker.net.study.IObjectLearningAlgo;
import com.rakovpublic.jneuropallium.worker.net.study.IResultComparingStrategy;
import com.rakovpublic.jneuropallium.worker.net.study.StudyingAlgoFactory;
import com.rakovpublic.jneuropallium.worker.neuron.impl.cycleprocessing.ProcessingFrequency;
import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;
import com.rakovpublic.jneuropallium.worker.util.JarClassLoaderService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.List;

public class LocalApplication implements IApplication {
    private static final Logger logger = LogManager.getLogger(LocalApplication.class);

    @Override
    public void startApplication(IContext context, JarClassLoaderService classLoaderService) {
        StructBuilder structBuilder = new StructBuilder();
        String layerPath = context.getProperty("configuration.input.layermeta");

        String classesImplemented = context.getProperty("configuration.neuronnet.classes");
        if (classesImplemented != null && classesImplemented.length() > 1) {
            for (String className : classesImplemented.split(",")) {
                if (!classLoaderService.containsClass(className)) {
                    logger.error("Cannot find class " + className + " in provided jar");
                    return;
                }

            }
        }


        String storageJson = context.getProperty("configuration.storage.json");
        IStorage fs = getStorage(storageJson);
        String inputLoadingStrategy = context.getProperty("configuration.input.loadingstrategy");
        Integer historySlow = Integer.parseInt(context.getProperty("configuration.history.slow.runs"));
        Long historyFast = Long.parseLong(context.getProperty("configuration.history.fast.runs"));
        IInputLoadingStrategy inputLoadingStrategyMain = this.getLoadingStrategy(inputLoadingStrategy);
        IInputResolver inputResolver = new InMemoryInputResolver(new InMemorySignalPersistStorage(), new InMemorySignalHistoryStorage(historySlow, historyFast), inputLoadingStrategyMain);
        String inputs = context.getProperty("configuration.input.inputs");
        for (InputData inputData : this.getInputs(inputs)) {
            inputResolver.registerInput(inputData.getiInputSource(), inputData.isMandatory(), inputData.getInitStrategy());
        }
        structBuilder.withHiddenInputMeta(inputResolver);
        structBuilder.withLayersMeta(new FileLayersMeta<>(fs.getItem(layerPath), fs));
        StructMeta meta = structBuilder.build();
        boolean isTeacherStudying = Boolean.parseBoolean(context.getProperty("configuration.isteacherstudying"));

        Long currentRun = 0l;
        Long maxRun = Long.valueOf(context.getProperty("configuration.maxRun"));
        Boolean isInfinite = Boolean.valueOf(context.getProperty("configuration.infiniteRun"));
        IOutputAggregator outputAggregator = null;
        String outputAggregatorClass = context.getProperty("configuration.outputAggregator");
        try {
            outputAggregator = (IOutputAggregator) Class.forName(outputAggregatorClass).getDeclaredConstructor().newInstance();
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | InstantiationException |
                IllegalAccessException e) {
            logger.error("cannot create output aggregator object", e);
        }
        for (; currentRun < maxRun || isInfinite; currentRun++) {

            HashMap<String, List<IResultSignal>> desiredResult = inputResolver.getDesiredResult();
            if (isTeacherStudying && desiredResult != null) {
                IResultComparingStrategy resultComparingStrategy = null;
                String resultComparingStrategyClass = context.getProperty("configuration.resultComparingStrategyClass");
                try {
                    resultComparingStrategy = (IResultComparingStrategy) Class.forName(resultComparingStrategyClass).getDeclaredConstructor().newInstance();
                } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                        InstantiationException | IllegalAccessException e) {
                    logger.error("cannot create result comparing strategy object", e);
                }
                String algoType = context.getProperty("configuration.studyingalgotype");
                for (; currentRun < maxRun || isInfinite; currentRun++) {
                    //Supervised learning
                    if (algoType != null && resultComparingStrategy != null) {
                        List<IResult> idsToFix;
                        if (algoType.equals("direct")) {
                            IDirectLearningAlgorithm directLearningAlgorithm = StudyingAlgoFactory.getDirectStudyingAlgo();
                            IResultLayer lr = process(meta);
                            while ((idsToFix = resultComparingStrategy.getIdsStudy(lr.interpretResult(), desiredResult)).size() > 0) {
                                meta.getInputResolver().saveHistory();
                                meta.getInputResolver().getSignalPersistStorage().cleanMiddleLayerSignals();
                                meta.learn(directLearningAlgorithm.learn(meta, desiredResult));
                                lr = process(meta);
                            }

                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getRun(), context);
                            meta.getInputResolver().saveHistory();
                            meta.getInputResolver().populateInput();
                        } else if (algoType.equals("object")) {
                            IObjectLearningAlgo iObjectStudyingAlgo = StudyingAlgoFactory.getObjectStudyingAlgo();
                            IResultLayer lr = process(meta);
                            while ((idsToFix = resultComparingStrategy.getIdsStudy(lr.interpretResult(), desiredResult)).size() > 0) {
                                meta.getInputResolver().saveHistory();
                                meta.getInputResolver().getSignalPersistStorage().cleanMiddleLayerSignals();
                                Integer layerId = meta.getResultLayer().getID();
                                HashMap<Long, List<ISignal>> studyMap = new HashMap<>();
                                for (IResult res : idsToFix) {
                                    studyMap.put(res.getNeuronId(), iObjectStudyingAlgo.getLearningSignals(desiredResult, meta));
                                }
                                HashMap<Integer, HashMap<Long, List<ISignal>>> studyingRequest = new HashMap<>();
                                studyingRequest.put(layerId, studyMap);
                                meta.getInputResolver().getSignalPersistStorage().cleanMiddleLayerSignals();
                                inputResolver.getSignalPersistStorage().putSignals(studyingRequest);
                                lr = process(meta);
                            }
                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getRun(), context);
                            meta.getInputResolver().saveHistory();
                            meta.getInputResolver().populateInput();
                        }
                        //Unsupervised or reinforced learning
                    } else {
                        IResultLayer lr = process(meta);
                        outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getRun(), context);
                        meta.getInputResolver().saveHistory();
                        meta.getInputResolver().populateInput();

                    }
                }
            } else {
                IResultResolver resultResolver = null;
                HashMap<String, StructMeta> discriminators = new HashMap<String, StructMeta>();
                Integer discriminatorsAmount = Integer.parseInt(context.getProperty("configuration.discriminatorsAmount"));
                ResultLayerHolder resultLayerHolder = new ResultLayerHolder();
                for (int i = 0; i < discriminatorsAmount; i++) {
                    String inputLoadingStrategyDiscriminator = context.getProperty("configuration.input.loadingstrategy.discriminator." + i);
                    String nameDiscriminator = context.getProperty("configuration.name.discriminator." + i);
                    Long discriminatorEpoch = Long.parseLong(context.getProperty("configuration.fast.runs.discriminator." + i));
                    Integer discriminatorLoop = Integer.parseInt(context.getProperty("configuration.slow.runs.discriminator." + i));
                    Integer historySlowDiscriminator = Integer.parseInt(context.getProperty("configuration.history.slow.runs.discriminator." + i));
                    Long historyFastDiscriminator = Long.parseLong(context.getProperty("configuration.history.fast.runs.discriminator." + i));
                    String layerPathDiscriminator = context.getProperty("configuration.input.layermeta.discriminator." + i);
                    String initStrategyDiscriminatorResult = context.getProperty("configuration.input.initStrategy.result.discriminator." + i);
                    String initStrategyDiscriminatorSource = context.getProperty("configuration.input.initStrategy.source.discriminator." + i);
                    String initStrategyDiscriminatorCallback = context.getProperty("configuration.input.initStrategy.callback.discriminator." + i);
                    IInputResolver inputResolverDiscriminator = new InMemoryInputResolver(new InMemorySignalPersistStorage(), new InMemorySignalHistoryStorage(historySlowDiscriminator, historyFastDiscriminator), this.getLoadingStrategy(inputLoadingStrategyDiscriminator));
                    InMemoryInitInput inMemoryInitInput = new InMemoryInitInputImpl(nameDiscriminator);
                    InMemoryDiscriminatorResultSignals inMemoryDiscriminatorResultSignals = new InMemoryDiscriminatorResultSignals(inMemoryInitInput, nameDiscriminator + "Result", resultLayerHolder, new ProcessingFrequency(1l, 1));
                    InMemoryDiscriminatorSourceSignals inMemoryDiscriminatorSourceSignals = new InMemoryDiscriminatorSourceSignals(inputLoadingStrategyMain, discriminatorEpoch, discriminatorLoop, nameDiscriminator + "Input", new ProcessingFrequency(1l, 1));
                    inputResolverDiscriminator.registerInput(inMemoryDiscriminatorResultSignals, true, getInputInitStrategy(initStrategyDiscriminatorResult));
                    inputResolverDiscriminator.registerInput(inMemoryDiscriminatorSourceSignals, true, getInputInitStrategy(initStrategyDiscriminatorSource));
                    inputResolver.registerInput(inMemoryInitInput, false, getInputInitStrategy(initStrategyDiscriminatorCallback));
                    inputResolverDiscriminator.populateInput();
                    StructBuilder structBuilderDiscriminator = new StructBuilder();
                    structBuilderDiscriminator.withHiddenInputMeta(inputResolverDiscriminator);
                    structBuilderDiscriminator.withLayersMeta(new FileLayersMeta<>(fs.getItem(layerPathDiscriminator), fs));
                    discriminators.put(nameDiscriminator, structBuilder.build());
                }
                resultResolver = new SimpleResultResolver();
                while (true) {
                    IResultLayer lr = process(meta);
                    resultLayerHolder.setResultLayer(lr);
                    if (resultResolver.resolveResult(meta, discriminators)) {
                        outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getRun(), context);
                    }
                    meta.getInputResolver().saveHistory();
                    meta.getInputResolver().populateInput();
                }

            }
        }


    }


    private IResultLayer process(StructMeta meta) {
        int i = 0;
        for (ILayerMeta met : meta.getLayers()) {
            LayerBuilder lb = new LayerBuilder();
            lb.withLayer(met);
            lb.withInput(meta.getInputResolver());
            ILayer layer = lb.build();
            if (layer.validateGlobal() && layer.validateLocal()) {
                logger.error("Layer validation rules violation");
            }
            layer.process();
            while (!layer.isProcessed()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
            layer.dumpNeurons(met);
        }
        IResultLayerMeta reMeta = meta.getResultLayer();
        LayerBuilder lb = new LayerBuilder();
        lb.withLayer(reMeta);
        lb.withInput(meta.getInputResolver());
        IResultLayer layer = lb.buildResultLayer();
        layer.process();
        layer.dumpNeurons(reMeta);
        return layer;


    }


    private IResultResolver getResultResolver(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        IResultResolver result = null;
        try {
            result = (IResultResolver) mapper.readValue(jobject.getAsJsonObject("resultResolver").getAsString(), Class.forName(jobject.getAsJsonPrimitive("resultResolverClass").getAsString()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse loading strategy  " + json, e);
        }
        return result;
    }

    private IInputLoadingStrategy getLoadingStrategy(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        IInputLoadingStrategy result = null;
        try {
            result = (IInputLoadingStrategy) mapper.readValue(jobject.getAsJsonObject("loadingStrategy").getAsString(), Class.forName(jobject.getAsJsonPrimitive("loadingStrategyClass").getAsString()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse loading strategy  " + json, e);
        }
        return result;
    }

    private InputInitStrategy getInputInitStrategy(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        InputInitStrategy result = null;
        try {
            result = (InputInitStrategy) mapper.readValue(jobject.getAsJsonObject("initStrategy").getAsString(), Class.forName(jobject.getAsJsonPrimitive("initStrategyClass").getAsString()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse init strategy  " + json, e);
        }
        return result;
    }

    private IStorage getStorage(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new JsonParser().parse(json);
        JsonObject jobject = jelement.getAsJsonObject();
        IStorage result = null;
        try {
            result = (IStorage) mapper.readValue(jobject.getAsJsonObject("storage").getAsString(), Class.forName(jobject.getAsJsonPrimitive("storageClass").getAsString()));
        } catch (JsonProcessingException | ClassNotFoundException e) {
            logger.error("Cannot parse init strategy  " + json, e);
        }
        return result;
    }


    private List<InputData> getInputs(String json) {
        ObjectMapper mapper = new ObjectMapper();
        List<InputData> result = null;
        try {
            result = mapper.readValue(json, InputArray.class).getInputData();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Cannot parse json " + json, e);
        }
        return result;
    }


}
