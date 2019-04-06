package synchronizer;

import net.storages.IInputMeta;
import net.storages.ILayersMeta;

public interface IRunner {
    void process(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta);
}
