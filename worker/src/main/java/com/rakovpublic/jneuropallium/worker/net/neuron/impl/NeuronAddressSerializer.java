/*
 * Copyright (c) 2024. Rakovskyi Dmytro
 */

package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.io.StringWriter;

public class NeuronAddressSerializer extends JsonSerializer<NeuronAddress> {
    @Override
    public void serialize(NeuronAddress neuronAddress, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        StringWriter writer = new StringWriter();
        ObjectMapper mapper = new ObjectMapper();
        String res = mapper.writeValueAsString(neuronAddress.getLayerId() + ":" + neuronAddress.getNeuronId());
        jsonGenerator.writeFieldName(neuronAddress.getLayerId() + ":" + neuronAddress.getNeuronId());
    }
}
