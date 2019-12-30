package net.storages;

import java.io.Serializable;
import java.util.List;

public interface ILayersMeta extends Serializable {
    List<ILayerMeta> getLayers();
    IResultLayerMeta getResultLayer();
}
