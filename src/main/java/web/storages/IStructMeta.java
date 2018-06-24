package web.storages;

import web.signals.ISignal;
import web.study.IStudyingRequest;

import java.util.HashMap;
import java.util.List;

public interface IStructMeta extends IStorageMeta {
    List<ILayerMeta> getLayers();

    HashMap<String, List<ISignal>> getInputs(int layerId);

    void saveResults(int layerId, HashMap<String, List<ISignal>> meta);

    void study(List<IStudyingRequest> requests);
}
