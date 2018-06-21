package web.storages;

import web.signals.ISignal;

import java.util.HashMap;
import java.util.List;

public interface IInputMeta<K> extends IStorageMeta {
    <S extends ISignal>void registerSerializer(ISerializer<S,K> serializer,Class<S> clazz);
    HashMap<String,List<ISignal>> readInputs();

}
