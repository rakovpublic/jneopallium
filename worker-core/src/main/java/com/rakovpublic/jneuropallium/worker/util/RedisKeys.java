/*
 * Copyright (c) 2026. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.util;

/**
 * Single source of truth for the Redis key layout of one neuron net.
 * <p>
 * Only plain Redis data types are used - no RedisJSON module is required:
 * <ul>
 *   <li>{@code &lt;net&gt;_properties}          HASH  - runtime properties read by {@code RedisContext}</li>
 *   <li>{@code &lt;net&gt;_layerIds}             LIST  - layer ids in processing order</li>
 *   <li>{@code &lt;net&gt;_layer_meta_&lt;L&gt;}       HASH  - layer meta params</li>
 *   <li>{@code &lt;net&gt;_layer_neurons_&lt;L&gt;}    HASH  - neuronId -&gt; neuron JSON</li>
 *   <li>{@code &lt;net&gt;_layer_index_&lt;L&gt;}      ZSET  - neuronId ordered index, makes a range O(log N + k)</li>
 *   <li>{@code &lt;net&gt;_signals_&lt;L&gt;}          HASH  - neuronId -&gt; JSON array of pending signals</li>
 *   <li>{@code &lt;net&gt;_history_&lt;L&gt;_&lt;e&gt;_&lt;l&gt;_&lt;n&gt;} STRING - JSON array of signals of one past step</li>
 *   <li>{@code &lt;net&gt;_run}                 HASH  - epoch / loop / cycle neuron mapping</li>
 * </ul>
 */
public final class RedisKeys {
    public static final int RESULT_LAYER_ID = Integer.MAX_VALUE;

    private RedisKeys() {
    }

    public static String properties(String net) {
        return net + "_properties";
    }

    public static String layerIds(String net) {
        return net + "_layerIds";
    }

    public static String layerMeta(String net, int layerId) {
        return net + "_layer_meta_" + layerId;
    }

    public static String layerNeurons(String net, int layerId) {
        return net + "_layer_neurons_" + layerId;
    }

    public static String layerIndex(String net, int layerId) {
        return net + "_layer_index_" + layerId;
    }

    public static String signals(String net, int layerId) {
        return net + "_signals_" + layerId;
    }

    public static String history(String net, int layerId, long epoch, int loop, long neuronId) {
        return net + "_history_" + layerId + "_" + epoch + "_" + loop + "_" + neuronId;
    }

    public static String run(String net) {
        return net + "_run";
    }

    public static String inputQueue(String net, String inputName) {
        return net + "_input_" + inputName;
    }
}
