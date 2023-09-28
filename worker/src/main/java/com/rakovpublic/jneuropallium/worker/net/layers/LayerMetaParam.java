/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers;

public class LayerMetaParam <K>{
    private K param;
    private String paramClass;

    public LayerMetaParam(K param) {
        this.param = param;
        paramClass = param.getClass().getCanonicalName();
    }

    public K getParam() {
        return param;
    }

    public void setParam(K param) {
        this.param = param;
    }

    public String getParamClass() {
        return paramClass;
    }
}
