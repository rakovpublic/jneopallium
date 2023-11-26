package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;

import java.io.IOException;

public class JSONProcCovertor implements Converter<String, ISignalProcessor> {

    public JSONProcCovertor() {
    }

    @Override
    public ISignalProcessor convert(String s) {
        JsonElement jelement = new com.google.gson.JsonParser().parse(s);
        JsonObject jobject = jelement.getAsJsonObject();
        String cl = jobject.getAsJsonPrimitive("signalProcessorClass").getAsString();
        ObjectMapper mapper = new ObjectMapper();
        try {
            return (ISignalProcessor) mapper.readValue(s, Class.forName(cl));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        //TODO: add logger
        return null;
    }

    @Override
    public JavaType getInputType(TypeFactory typeFactory) {
        return typeFactory.constructFromCanonical(String.class.getCanonicalName());
    }

    @Override
    public JavaType getOutputType(TypeFactory typeFactory) {
        return typeFactory.constructFromCanonical(ISignalProcessor.class.getCanonicalName());
    }
}
