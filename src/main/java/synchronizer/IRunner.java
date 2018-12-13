package synchronizer;

import web.storages.IInputMeta;
import web.storages.ILayersMeta;

public interface IRunner {
    void process(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta);
}
