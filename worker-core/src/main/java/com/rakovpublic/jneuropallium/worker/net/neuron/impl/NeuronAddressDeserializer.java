/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;

import java.io.IOException;

public class NeuronAddressDeserializer extends KeyDeserializer {
    @Override
    public NeuronAddress deserializeKey(String s, DeserializationContext deserializationContext) throws IOException {
        String[] res = s.split(":");
        if (res.length < 2) {
            throw new IOException("Invalid NeuronAddress key format (expected 'layerId:neuronId'): " + s);
        }
        return new NeuronAddress(Integer.parseInt(res[0]), Long.parseLong(res[1]));
    }
}
