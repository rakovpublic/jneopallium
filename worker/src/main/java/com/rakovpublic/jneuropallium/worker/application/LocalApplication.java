package com.rakovpublic.jneuropallium.worker.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InMemoryInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InputArray;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InputData;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerBuilder;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
import com.rakovpublic.jneuropallium.worker.net.signals.ISignal;
import com.rakovpublic.jneuropallium.worker.net.storages.IInputLoadingStrategy;
import com.rakovpublic.jneuropallium.worker.net.storages.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.file.FileLayersMeta;
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory.InMemorySignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory.InMemorySignalPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructBuilder;
import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructMeta;
import com.rakovpublic.jneuropallium.worker.net.study.IDirectLearningAlgorithm;
import com.rakovpublic.jneuropallium.worker.net.study.IObjectLearningAlgo;
import com.rakovpublic.jneuropallium.worker.net.study.IResultComparingStrategy;
import com.rakovpublic.jneuropallium.worker.net.study.StudyingAlgoFactory;
import com.rakovpublic.jneuropallium.worker.synchronizer.IContext;
import com.rakovpublic.jneuropallium.worker.synchronizer.utils.InstantiationUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LocalApplication implements IApplication {
    private static final Logger logger = LogManager.getLogger(LocalApplication.class);

    @Override
    public void startApplication(IContext context) {
        StructBuilder structBuilder = new StructBuilder();
        String layerPath = context.getProperty("configuration.input.layermeta");


        String fileSystemClass = context.getProperty("configuration.storage.class");
        Class<IStorage> clazz = null;
        try {
            clazz = (Class<IStorage>) Class.forName(fileSystemClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            logger.error("Cannot find file system class" + fileSystemClass, e);
            return;
        }
        String fileSystemConstructorArgs = context.getProperty("configuration.storage.constructor.args");
        String fileSystemConstructorArgsType = context.getProperty("configuration.storage.constructor.args.types");
        IStorage fs = InstantiationUtils.<IStorage>getObject(clazz, getObjects(fileSystemConstructorArgs), getTypes(fileSystemConstructorArgsType));
        String inputLoadingStrategy = context.getProperty("configuration.input.loadingstrategy");
        IInputResolver inputResolver = new InMemoryInputResolver(new InMemorySignalPersistStorage(), new InMemorySignalHistoryStorage(), this.getLoadingStrategy(inputLoadingStrategy));
        String inputs = context.getProperty("configuration.input.inputs");
        for (InputData inputData : this.getInputs(inputs)) {
            inputResolver.registerInput(inputData.getiInputSource(), inputData.isMandatory(), inputData.getInitStrategy(), inputData.getAmountOfRuns());
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
                                meta.study(directLearningAlgorithm.learn(meta, desiredResult));
                                lr = process(meta);
                            }

                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getEpoch(), context);
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
                                    studyMap.put(res.getNeuronId(), iObjectStudyingAlgo.getLearningSignals(desiredResult,meta));
                                }
                                HashMap<Integer, HashMap<Long, List<ISignal>>> studyingRequest = new HashMap<>();
                                studyingRequest.put(layerId, studyMap);
                                meta.getInputResolver().getSignalPersistStorage().cleanMiddleLayerSignals();
                                inputResolver.getSignalPersistStorage().putSignals(studyingRequest);
                                lr = process(meta);
                            }
                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getEpoch(), context);
                            meta.getInputResolver().saveHistory();
                            meta.getInputResolver().populateInput();
                        }
                        //Unsupervised or reinforced learning
                    } else {
                        IResultLayer lr = process(meta);
                        outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getEpoch(), context);
                        meta.getInputResolver().saveHistory();
                        meta.getInputResolver().populateInput();
                    }
                }
            } else {
                //Unsupervised or reinforced learning
                while (true) {
                    IResultLayer lr = process(meta);
                    outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(), meta.getInputResolver().getEpoch(), context);
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
                    e.printStackTrace();
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

    private List<Class<?>> getTypes(String str) {

        List<Class<?>> reuslt = new ArrayList<>();
        if (str.equals("empty")) {
            return reuslt;
        }
        try {
            if (str.contains(":")) {
                String[] parts = str.split(":");
                for (String cl : parts) {
                    reuslt.add(Class.forName(cl));
                }
            } else {
                reuslt.add(Class.forName(str));
            }
        } catch (ClassNotFoundException e) {
            logger.error("Cannot find class for name: " + str, e);
        }
        return reuslt;

    }


    //TODO: refactore it
    private Object[] getObjects(String str) {
        if (str.equals("empty")) {
            return new Object[0];
        }
        Object[] obj = null;
        try {
            byte b[] = str.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            obj = (Object[]) si.readObject();
        } catch (Exception ex) {
            logger.error("Cannot deserialize string to object array ", ex);
        }
        return obj;

    }

    private IInputLoadingStrategy getLoadingStrategy(String json) {
        ObjectMapper mapper = new ObjectMapper();
        IInputLoadingStrategy result = null;
        try {
            result = mapper.readValue(json, IInputLoadingStrategy.class);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            logger.error("Cannot parse loading strategy  " + json, e);
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
