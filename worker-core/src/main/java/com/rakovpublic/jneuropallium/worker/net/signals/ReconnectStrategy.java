/*
 * Copyright (c) 2023. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.signals;

import com.rakovpublic.jneuropallium.worker.net.layers.ILayersMeta;

import java.util.HashMap;
import java.util.List;

public interface ReconnectStrategy {
    HashMap<Integer, HashMap<Long, HashMap<Integer, List<Long>>>> getNewConnections(ILayersMeta layersMeta, Integer layerToRemove);
}
