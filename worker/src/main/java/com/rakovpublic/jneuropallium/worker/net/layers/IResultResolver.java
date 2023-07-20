package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructMeta;

import java.util.HashMap;
import java.util.List;

public interface IResultResolver {
    void resolveResult(StructMeta targetNeuronNet, HashMap<String,StructMeta> discriminators, IResultLayer resultLayer);
}
