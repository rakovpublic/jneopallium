package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rakovpublic.jneuropallium.worker.exceptions.JSONParsingException;
import com.rakovpublic.jneuropallium.worker.neuron.ISignalChain;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class SignalChainDeserializer extends StdDeserializer<ISignalChain> {
    private static final Logger logger = LogManager.getLogger(SignalChainDeserializer.class);
    public SignalChainDeserializer() {
        this(null);
    }

    public SignalChainDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public ISignalChain deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            return (ISignalChain) mapper.readValue(jobject.getAsJsonObject("signalChain").toString(), Class.forName(jobject.getAsJsonPrimitive("signalChainClass").getAsString()));
        } catch (ClassNotFoundException e) {
            logger.error("Cannot deserialize signalChain " + jobject.getAsJsonObject("signalChain").toString() + " for class " + jobject.getAsJsonPrimitive("signalChainClass").getAsString(),e);
            throw new JSONParsingException(e.getMessage());
        }
    }
}
