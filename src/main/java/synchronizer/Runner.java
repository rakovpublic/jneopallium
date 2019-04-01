package synchronizer;

import net.storages.IInputMeta;
import net.storages.ILayersMeta;
import net.storages.structimpl.StructMeta;

public class Runner implements IRunner {
    @Override
    public void process(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta) {
        StructMeta structMeta= new StructMeta(initInputMeta,hiddenInputMeta,layersMeta);
    }
}
