package web.storages;

import java.util.List;

public interface IStorageMeta {
    List<ILayerMeta> getLayers();
    IInputMeta getInput();
}
