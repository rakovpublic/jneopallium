package application;

import net.layers.ILayer;
import net.layers.IResultLayer;
import net.layers.impl.LayerBuilder;
import net.signals.IResultSignal;
import net.signals.ISignal;
import net.storages.IInputMeta;
import net.storages.ILayerMeta;
import net.storages.IResultLayerMeta;
import net.storages.ISerializer;
import net.storages.file.FileLayersMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.signalstorages.file.FileInputMeta;
import net.storages.structimpl.StructBuilder;
import net.storages.structimpl.StructMeta;
import net.study.IStudyingAlgorithm;
import synchronizer.IContext;
import synchronizer.utils.InstantiationUtils;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
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
            IInputMeta inputMeta = new FileInputMeta(fs.getItem(inputPath), fs);
            structBuilder.withInitInputMeta(inputMeta);
            structBuilder.withHiddenInputMeta(inputMeta);
            structBuilder.withLayersMeta(new FileLayersMeta<>(fs.getItem(layerPath), fs));
            StructMeta meta = structBuilder.build();
            boolean isTeacherStudying = Boolean.valueOf(context.getProperty("configuration.isteacherstudying"));
            IStudyingAlgorithm algo = null;
            IResultSignal desiredResult = inputMeta.getDesiredResult();
            if (isTeacherStudying) {
                Object objst = getObject(context.getProperty("configuration.studyingalgo"));
                if (objst != null) {
                    algo = (IStudyingAlgorithm) objst;
                    IResultLayer iResultLayer;
                    while ((iResultLayer = process(meta))!=null && !iResultLayer.interpretResult().getResult().equals(desiredResult)) {
                        meta.study(((IStudyingAlgorithm) objst).study(meta, iResultLayer.interpretResult().getNeuronId()));
                        meta.getInputs(0).copyInputsToNextStep();
                        meta.getInputs(0).nextStep();
                    }
                } else {

                    while (!process(meta).interpretResult().getResult().equals(desiredResult)) {
                        meta.getInputs(0).copyInputsToNextStep();
                        meta.getInputs(0).nextStep();
                    }
                }
            } else {
                //TODO:add normal output
                System.out.println(process(meta).interpretResult().getResult().toString());
            }


        } else {

        }

    }

    private IResultLayer process(StructMeta meta) {
        int i = 0;
        for (ILayerMeta met : meta.getLayers()) {
            LayerBuilder lb = new LayerBuilder();
            lb.withLayer(met);
            lb.withInput(meta.getInputs(met.getID()));
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
            i++;
            layer.dumpResult(meta.getInputs(i));

        }
        IResultLayerMeta reMeta = meta.getResultLayer();
        LayerBuilder lb = new LayerBuilder();
        lb.withLayer(reMeta);
        lb.withInput(meta.getInputs(reMeta.getID()));
        IResultLayer layer = lb.buildResultLayer();
        layer.process();

        return layer;


    }

    private Class<?>[] getTypes(String str) {

        List<Class<?>> reuslt = new ArrayList<>();
        if(str.equals("empty")){
            return   (Class<?>[]) reuslt.toArray() ;
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
        return (Class<?>[]) reuslt.toArray();

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



}
