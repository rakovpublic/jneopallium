package com.rakovpublic.jneuropallium.worker.net.storages;

import java.util.HashMap;
import java.util.List;

public interface ReconnectStrategy {
    HashMap<Integer, HashMap<Long, HashMap<Integer, List<Long>>>> getNewConnections(ILayersMeta layersMeta, Integer layerToRemove);
}
