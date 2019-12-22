package application;

import net.layers.ILayer;
import net.layers.impl.LayerBuilder;
import net.neuron.impl.NeuronRunnerService;
import net.signals.ISignal;
import net.storages.*;
import net.storages.file.FileLayersMeta;
import net.storages.filesystem.IFileSystem;
import net.storages.signalstorages.file.FileInputMeta;
import net.storages.structimpl.StructBuilder;
import net.storages.structimpl.StructMeta;
import synchronizer.IContext;
import synchronizer.utils.InstantiationUtils;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.sql.Struct;
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
            String serializers = context.getProperty("configuration.serializers");
            IInputMeta inputMeta = new FileInputMeta(fs.getItem(inputPath), getSerializers(serializers), fs);
            structBuilder.withInitInputMeta(inputMeta);
            structBuilder.withHiddenInputMeta(inputMeta);
            structBuilder.withLayersMeta(new FileLayersMeta<>(fs.getItem(layerPath), fs));
            StructMeta meta = structBuilder.build();
            int i = 0;
            for (ILayerMeta met : meta.getLayers()) {
                LayerBuilder lb = new LayerBuilder();
                lb.withLayer(met);
                lb.withInput(meta.getInputs(i));
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


        } else {

        }

    }

    private Class<?>[] getTypes(String str) {
        List<Class<?>> reuslt = new ArrayList<>();
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

    private Object[] getObjects(String str) {
        Object[] obj=null;
        try {
            byte b[] = str.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
           obj = (Object[]) si.readObject();
        }catch (Exception ex){
            //TODO:Add logger
        }
        return obj;

    }

    private HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>> getSerializers(String str) {
        HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>> obj=null;
        try {
            byte b[] = str.getBytes();
            ByteArrayInputStream bi = new ByteArrayInputStream(b);
            ObjectInputStream si = new ObjectInputStream(bi);
            obj = (HashMap<Class<? extends ISignal>, ISerializer<? extends ISignal, String>>) si.readObject();
        }catch (Exception ex){
            //TODO:Add logger
        }
        return obj;
    }


}
