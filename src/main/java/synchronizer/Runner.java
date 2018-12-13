package synchronizer;

import web.storages.IInputMeta;
import web.storages.ILayersMeta;
import web.storages.structimpl.StructMeta;

public class Runner implements IRunner {
    @Override
    public void process(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta) {
        StructMeta structMeta= new StructMeta(initInputMeta,hiddenInputMeta,layersMeta);
    }
}
