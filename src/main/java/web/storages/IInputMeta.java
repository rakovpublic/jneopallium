package web.storages;

import web.signals.ISignal;


import java.io.File;
import java.util.HashMap;
import java.util.List;

public interface IInputMeta<K> extends IStorageMeta {
    <S extends ISignal>void registerSerializer(ISerializer<S,K> serializer,Class<S> clazz);
    HashMap<Long,List<ISignal>> readInputs(int layerId);
    void saveResults( HashMap<Long,List<ISignal>> signals,int layerId);
    void mergeResults(HashMap<Long,List<ISignal>> signals,int layerId);

}
