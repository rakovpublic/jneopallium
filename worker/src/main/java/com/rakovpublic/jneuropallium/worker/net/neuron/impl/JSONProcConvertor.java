package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rakovpublic.jneuropallium.worker.application.LocalApplication;
import com.rakovpublic.jneuropallium.worker.exceptions.JSONParsingException;
import com.rakovpublic.jneuropallium.worker.net.neuron.ISignalProcessor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class JSONProcConvertor implements Converter<String, ISignalProcessor> {
    private static final Logger logger = LogManager.getLogger(JSONProcConvertor.class);

    public JSONProcConvertor() {
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
            logger.error(e);
            throw new JSONParsingException(e.getMessage());
        } catch (ClassNotFoundException e) {
            logger.error(e);
            throw new JSONParsingException(e.getMessage());
        }
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
