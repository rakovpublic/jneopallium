package net.storages.structimpl;

import net.layers.ILayer;
import net.signals.ISignal;
import net.storages.*;
import net.study.IStudyingRequest;

import java.util.HashMap;
import java.util.List;

public class StructMeta implements IStructMeta {

    private IInputMeta initInputMeta;
    private IInputMeta hiddenInputMeta;
    private ILayersMeta layersMeta;

    public StructMeta(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta) {
        this.initInputMeta = initInputMeta;
        this.hiddenInputMeta = hiddenInputMeta;
        this.layersMeta = layersMeta;
    }

    @Override
    public List<ILayerMeta> getLayers() {
        return layersMeta.getLayers();
    }


    @Override
    public IInputMeta getInputs(int layerId) {
        if (layerId == 0) {
            return initInputMeta;
        }
        return hiddenInputMeta;
    }

    @Override
    public void saveResults(int layerId, HashMap<String, List<ISignal>> meta) {
        hiddenInputMeta.saveResults(meta, layerId);
    }

    @Override
    public void study(List<IStudyingRequest> requests) {
        //TODO: Add implementation
    }

    @Override
    public IResultLayerMeta getResultLayer() {
        return layersMeta.getResultLayer();
    }


}
