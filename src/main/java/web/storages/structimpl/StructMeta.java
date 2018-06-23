package web.storages.structimpl;

import exceptions.IncorrectFilePathForStorageException;
import exceptions.LayersFolderIsEmptyOrNotExistsException;
import web.signals.ISignal;
import web.storages.IInputMeta;
import web.storages.ILayerMeta;
import web.storages.ILayersMeta;
import web.storages.IStructMeta;

import java.io.File;
import java.util.*;

public class StructMeta implements IStructMeta {

    private IInputMeta initInputMeta;
    private IInputMeta hiddenInputMeta;
    private ILayersMeta layersMeta;

    StructMeta(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta) {
        this.initInputMeta = initInputMeta;
        this.hiddenInputMeta = hiddenInputMeta;
        this.layersMeta = layersMeta;
    }

    @Override
    public List<ILayerMeta> getLayers() {
        return layersMeta.getLayers();
    }

    @Override
    public HashMap<String, List<ISignal>> getInputs(int layerId) {
        if(layerId==0){
            return initInputMeta.readInputs(layerId);
        }
        return hiddenInputMeta.readInputs(layerId);
    }

    @Override
    public void saveResults(int layerId, HashMap<String, List<ISignal>> meta) {
        hiddenInputMeta.saveResults(meta,layerId);
    }


}
