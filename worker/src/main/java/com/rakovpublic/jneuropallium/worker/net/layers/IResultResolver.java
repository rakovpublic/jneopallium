package com.rakovpublic.jneuropallium.worker.net.layers;

import com.rakovpublic.jneuropallium.worker.net.storages.structimpl.StructMeta;

import java.util.HashMap;

public interface IResultResolver {
    boolean resolveResult(StructMeta targetNeuronNet, HashMap<String, StructMeta> discriminators);
}
