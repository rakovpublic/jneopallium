/*
 * Copyright (c) 2026. Rakovskyi Dmytro. BSD 3-Clause.
 */
package com.rakovpublic.jneuropallium.worker.demo.industrialfmi;

import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaBridgeConfig;
import com.rakovpublic.jneuropallium.worker.net.neuron.impl.industrial.opcua.OpcUaNodeBinding;

import java.util.List;

public final class IndustrialFmiInputRegistry {
    private IndustrialFmiInputRegistry() {}

    public static List<OpcUaNodeBinding> readBindings(OpcUaBridgeConfig config) {
        return config.reads().stream().map(OpcUaNodeBinding::new).toList();
    }
}
