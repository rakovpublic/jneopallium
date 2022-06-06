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
import com.rakovpublic.jneuropallium.worker.net.storages.filesystem.IFileSystem;
import com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory.InMemorySignalHistoryStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.signalstorages.inmemory.InMemorySignalPersistStorage;
import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructBuilder;
import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructMeta;
import com.rakovpublic.jneuropallium.worker.net.study.IDirectStudyingAlgorithm;
import com.rakovpublic.jneuropallium.worker.net.study.IObjectStudyingAlgo;
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
        String inputType = context.getProperty("configuration.input.type");
        String inputPath = context.getProperty("configuration.input.path");
        StructBuilder structBuilder = new StructBuilder();
        String layerPath = context.getProperty("configuration.input.layermeta");

        if (inputType.equals("fileSystem")) {
            String fileSystemClass = context.getProperty("configuration.filesystem.class");
            Class<IFileSystem> clazz = null;
            try {
                clazz = (Class<IFileSystem>) Class.forName(fileSystemClass);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                logger.error("Cannot find file system class" + fileSystemClass,e);
                return;
            }
            String fileSystemConstructorArgs = context.getProperty("configuration.filesystem.constructor.args");
            String fileSystemConstructorArgsType = context.getProperty("configuration.filesystem.constructor.args.types");
            IFileSystem fs = InstantiationUtils.<IFileSystem>getObject(clazz, getObjects(fileSystemConstructorArgs), getTypes(fileSystemConstructorArgsType));
            String inputLoadingStrategy = context.getProperty("configuration.input.loadingstrategy");
            IInputResolver inputResolver = new InMemoryInputResolver(new InMemorySignalPersistStorage(), new InMemorySignalHistoryStorage(), this.getLoadingStrategy(inputLoadingStrategy));
            String inputs = context.getProperty("configuration.input.inputs");
            for (InputData inputData : this.getInputs(inputs)) {
                inputResolver.registerInput(inputData.getiInputSource(), inputData.isMandatory(), inputData.getInitStrategy(), inputData.getAmountOfRuns());
            }
            structBuilder.withHiddenInputMeta(inputResolver);
            structBuilder.withLayersMeta(new FileLayersMeta<>(fs.getItem(layerPath), fs));
            StructMeta meta = structBuilder.build();
            boolean isTeacherStudying = Boolean.valueOf(context.getProperty("configuration.isteacherstudying"));

            Long currentRun = 0l;
            Long maxRun = Long.valueOf(context.getProperty("configuration.maxRun"));
            Boolean isInfinite = Boolean.valueOf(context.getProperty("configuration.infiniteRun"));
            IOutputAggregator outputAggregator = null;
            String outputAggregatorClass = context.getProperty("configuration.outputAggregator");
            try {
                outputAggregator = (IOutputAggregator) Class.forName(outputAggregatorClass).getDeclaredConstructor().newInstance();
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                //TODO:Add logger
            } catch (InvocationTargetException e) {
                //TODO:Add logger
            } catch (InstantiationException e) {
                //TODO:Add logger
            } catch (IllegalAccessException e) {
                //TODO:Add logger
            }
            for (; currentRun < maxRun || isInfinite; currentRun++) {

                HashMap<String, List<IResultSignal>> desiredResult = inputResolver.getDesiredResult();
                if (isTeacherStudying && desiredResult != null) {
                    IResultComparingStrategy resultComparingStrategy = null;
                    String resultComparingStrategyClass = context.getProperty("configuration.resultComparingStrategyClass");
                    try {
                        resultComparingStrategy = (IResultComparingStrategy) Class.forName(resultComparingStrategyClass).getDeclaredConstructor().newInstance();
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        //TODO:Add logger
                    } catch (InvocationTargetException e) {
                        //TODO:Add logger
                    } catch (InstantiationException e) {
                        //TODO:Add logger
                    } catch (IllegalAccessException e) {
                        //TODO:Add logger
                    }
                    String algoType = context.getProperty("configuration.studyingalgotype");
                    if (algoType != null && resultComparingStrategy != null) {
                        List<IResult> idsToFix;
                        if (algoType.equals("direct")) {
                            IDirectStudyingAlgorithm directStudyingAlgorithm = StudyingAlgoFactory.getDirectStudyingAlgo();
                            IResultLayer lr = process(meta);
                            while ((idsToFix = resultComparingStrategy.getIdsStudy(process(meta).interpretResult(), desiredResult)).size() > 0) {
                                for (IResult res : idsToFix) {
                                    meta.study(directStudyingAlgorithm.study(meta, res.getNeuronId()));
                                }
                                meta.getInputResolver().saveHistory();
                                meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                                meta.getInputResolver().populateInput();
                            }
                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(),meta.getInputResolver().getCurrentRun(),context);
                        } else if (algoType.equals("object")) {
                            IObjectStudyingAlgo iObjectStudyingAlgo = StudyingAlgoFactory.getObjectStudyingAlgo();
                            IResultLayer lr = process(meta);
                            while ((idsToFix = resultComparingStrategy.getIdsStudy(lr.interpretResult(), desiredResult)).size() > 0) {
                                meta.getInputResolver().saveHistory();
                                meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                                meta.getInputResolver().populateInput();
                                Integer layerId = meta.getResultLayer().getID();
                                HashMap<Long, List<ISignal>> studyMap = new HashMap<>();
                                for (IResult res : idsToFix) {
                                    studyMap.put(res.getNeuronId(), iObjectStudyingAlgo.getStudyingSignals());
                                }
                                HashMap<Integer, HashMap<Long, List<ISignal>>> studyingRequest = new HashMap<>();
                                studyingRequest.put(layerId, studyMap);
                                inputResolver.getSignalPersistStorage().putSignals(studyingRequest);
                            }
                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(),meta.getInputResolver().getCurrentRun(),context);
                        }
                    } else {
                        for (; currentRun < maxRun || isInfinite; currentRun++) {
                            IResultLayer lr = process(meta);
                            outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(),meta.getInputResolver().getCurrentRun(),context);
                            meta.getInputResolver().saveHistory();
                            meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                            meta.getInputResolver().populateInput();
                        }
                    }
                } else {
                    //TODO:add normal output
                    while (true){
                        IResultLayer lr = process(meta);
                        outputAggregator.save(lr.interpretResult(), System.currentTimeMillis(),meta.getInputResolver().getCurrentRun(),context);
                        meta.getInputResolver().saveHistory();
                        meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                        meta.getInputResolver().populateInput();
                    }

                }
            }

        } else {
            // TODO: refactor architecture for different input types

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
            logger.error("Cannot find class for name: " +str, e);
        }
        return reuslt;

    }

    private Object getObject(String str) {
        if (str == null) {
            return null;
        }
        Object obj = null;
        try {
            byte b[] = str.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            obj = si.readObject();
        } catch (Exception ex) {
            //TODO:Add logger
        }
        return obj;

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
            //TODO:Add logger
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
            logger.error("Cannot parse loading strategy  " + json,e);
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
            logger.error("Cannot parse json " + json,e);
        }
        return result;
    }


}
