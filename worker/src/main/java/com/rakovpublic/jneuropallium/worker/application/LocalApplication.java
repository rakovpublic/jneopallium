package com.rakovpublic.jneuropallium.worker.application;

import com.rakovpublic.jneuropallium.worker.net.layers.IInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.IResult;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayer;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InMemoryInputResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.InputData;
import com.rakovpublic.jneuropallium.worker.net.layers.impl.LayerBuilder;
import com.rakovpublic.jneuropallium.worker.net.signals.IResultSignal;
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

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class LocalApplication implements IApplication {
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
                //TODO:add logger
                return;
            }
            String fileSystemConstructorArgs = context.getProperty("configuration.filesystem.constructor.args");
            String fileSystemConstructorArgsType = context.getProperty("configuration.filesystem.constructor.args.types");
            IFileSystem fs = InstantiationUtils.<IFileSystem>getObject(clazz, getObjects(fileSystemConstructorArgs), getTypes(fileSystemConstructorArgsType));
            String inputLoadingStrategy=context.getProperty("configuration.input.loadingstrategy");
            IInputResolver inputResolver = new InMemoryInputResolver(new InMemorySignalPersistStorage(), new InMemorySignalHistoryStorage(),this.getLoadingStrategy(inputLoadingStrategy));
            String inputs=context.getProperty("configuration.input.inputs");
            for(InputData inputData: this.getInputs(inputs)){
                inputResolver.registerInput(inputData.getiInputSource(),inputData.isMandatory(),inputData.getInitStrategy(),inputData.getAmountOfRuns());
            }
            structBuilder.withHiddenInputMeta(inputResolver);
            structBuilder.withLayersMeta(new FileLayersMeta<>(fs.getItem(layerPath), fs));
            StructMeta meta = structBuilder.build();
            boolean isTeacherStudying = Boolean.valueOf(context.getProperty("configuration.isteacherstudying"));

            Long currentRun = 0l;
            Long maxRun = Long.valueOf(context.getProperty("configuration.maxRun"));
            Boolean isInfinite = Boolean.valueOf(context.getProperty("configuration.infiniteRun"));

            for (; currentRun < maxRun || isInfinite; currentRun++) {

                HashMap<String, List<IResultSignal>> desiredResult = inputResolver.getDesiredResult();
                if (isTeacherStudying && desiredResult != null) {
                    IResultComparingStrategy resultComparingStrategy=null;
                    String jsonResultComparingStrategy =context.getProperty("configuration.jsonResultComparingStrategy");
                    //TODO:add json parsing with wrapper
                    String algoType = context.getProperty("configuration.studyingalgotype");
                    IResultLayer iResultLayer;
                    if (algoType != null) {
                        List<IResult> idsToFix;
                        if (algoType.equals("direct")) {
                            IDirectStudyingAlgorithm directStudyingAlgorithm = StudyingAlgoFactory.getDirectStudyingAlgo();

                            while ((idsToFix=resultComparingStrategy.getIdsStudy(process(meta).interpretResult(),desiredResult)).size()>0) {
                                for (IResult res : idsToFix) {
                                    meta.study(directStudyingAlgorithm.study(meta, res.getNeuronId()));
                                }
                                meta.getInputResolver().saveHistory();
                                meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                                meta.getInputResolver().populateInput();
                            }
                        } else if (algoType.equals("object")) {
                            IObjectStudyingAlgo iObjectStudyingAlgo = StudyingAlgoFactory.getObjectStudyingAlgo();
                            while ((idsToFix=resultComparingStrategy.getIdsStudy(process(meta).interpretResult(),desiredResult)).size()>0) {
                                meta.getInputResolver().saveHistory();
                                meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                                meta.getInputResolver().populateInput();
                                for (IResult res : idsToFix) {
                                    inputResolver.getSignalPersistStorage().putSignals(iObjectStudyingAlgo.study(res.getNeuronId()));
                                }
                            }
                        }
                    } else {
                        for (; currentRun < maxRun || isInfinite; currentRun++) {
                            meta.getInputResolver().saveHistory();
                            meta.getInputResolver().getSignalPersistStorage().cleanOutdatedSignals();
                            meta.getInputResolver().populateInput();
                        }
                    }
                } else {
                    //TODO:add normal output
                    IResultLayer lr = process(meta);
                    System.out.println(lr.interpretResult());
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
                //TODO: add logger invalid layer configuration and exception
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

        return layer;


    }

    private List<Class<?>> getTypes(String str) {

        List<Class<?>> reuslt = new ArrayList<>();
        if(str.equals("empty")){
            return    reuslt ;
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
            //TODO:Add logger
        }
        return  reuslt;

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


    private Object[] getObjects(String str) {
        if(str.equals("empty")){
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

    private IInputLoadingStrategy getLoadingStrategy(String json){
        //TODO: add parsing implementation
        return null;
    }
    private List<InputData> getInputs(String json){
        //TODO: add parsing implementation
        return null;
    }



}
