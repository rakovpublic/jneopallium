/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.layers.impl;

import com.rakovpublic.jneuropallium.worker.net.layers.DiscriminatorResultLayer;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayer;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultResolver;
import com.rakovpublic.jneuropallium.worker.net.layers.ILayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.IResultLayerMeta;
import com.rakovpublic.jneuropallium.worker.net.layers.StructMeta;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;

public class SimpleResultResolver implements IResultResolver {
    private static final Logger logger = LogManager.getLogger(SimpleResultResolver.class);

    @Override
    public boolean resolveResult(StructMeta targetNeuronNet, HashMap<String, StructMeta> discriminators) {
        for (String discriminatorName : discriminators.keySet()) {
            DiscriminatorResultLayer lr = process(discriminators.get(discriminatorName));
            if (!lr.hasPass()) {
                return false;
            }
        }
        return true;
    }


    private DiscriminatorResultLayer process(StructMeta meta) {
        for (ILayerMeta met : meta.getLayers()) {
            LayerBuilder lb = new LayerBuilder();
            lb.withLayer(met);
            lb.withInput(meta.getInputResolver());
            ILayer layer = lb.build();
            if (layer.validateGlobal() && layer.validateLocal()) {
                logger.error("Layer validation rules violation");
            }
            layer.process();
            while (!layer.isProcessed()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
            }
            layer.dumpNeurons(met);
        }
        IResultLayerMeta reMeta = meta.getResultLayer();
        LayerBuilder lb = new LayerBuilder();
        lb.withLayer(reMeta);
        lb.withInput(meta.getInputResolver());
        DiscriminatorResultLayer layer = (DiscriminatorResultLayer) lb.buildResultLayer();
        layer.process();
        layer.dumpNeurons(reMeta);
        return layer;


    }
}
