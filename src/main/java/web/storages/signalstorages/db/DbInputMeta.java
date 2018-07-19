package web.storages.signalstorages.db;

import web.signals.ISignal;
import web.storages.IInputMeta;
import web.storages.ISerializer;

import java.util.HashMap;
import java.util.List;

public class DbInputMeta implements IInputMeta {
    @Override
    public void registerSerializer(ISerializer serializer, Class clazz) {

    }

    @Override
    public HashMap<Long, List<ISignal>> readInputs(int layerId) {
        return null;
    }

    @Override
    public void mergeResults(HashMap signals, int layerId) {

    }

    @Override
    public void saveResults(HashMap signals, int layerId) {

    }
}
