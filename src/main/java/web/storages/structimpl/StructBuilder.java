package web.storages.structimpl;

import web.storages.IInputMeta;
import web.storages.ILayersMeta;

public class StructBuilder {
    private IInputMeta initInputMeta;
    private IInputMeta hiddenInputMeta;
    private ILayersMeta layersMeta;
    public StructBuilder() {

    }

    public StructBuilder(IInputMeta initInputMeta, IInputMeta hiddenInputMeta, ILayersMeta layersMeta) {
        this.initInputMeta = initInputMeta;
        this.hiddenInputMeta = hiddenInputMeta;
        this.layersMeta = layersMeta;
    }

    public StructBuilder withHiddenInputMeta(IInputMeta hiddenInputMeta){
        this.hiddenInputMeta=hiddenInputMeta;
        return this;
    }
    public StructBuilder withLayersMeta(ILayersMeta layersMeta){
        this.layersMeta=layersMeta;
        return this;
    }
    public StructBuilder withInitInputMeta(IInputMeta initInputMeta){
        this.initInputMeta=initInputMeta;
        return this;
    }
    public StructMeta build(){
        if(initInputMeta!=null&&hiddenInputMeta!=null&&layersMeta!=null){
            return new StructMeta(initInputMeta,hiddenInputMeta,layersMeta);
        }
        return null;
    }
}
