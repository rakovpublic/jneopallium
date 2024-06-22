package com.rakovpublic.jneuropallium.worker.net.neuron.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rakovpublic.jneuropallium.worker.exceptions.JSONParsingException;
import com.rakovpublic.jneuropallium.worker.net.neuron.IWeight;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

public class WeightDeserializer extends StdDeserializer<IWeight> {
    private static final Logger logger = LogManager.getLogger(WeightDeserializer.class);

    public WeightDeserializer() {
        this(null);
    }

    protected WeightDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public IWeight deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        Gson gson = new Gson();
        ObjectMapper mapper = new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            //return (IWeight) mapper.readValue(jobject.toString(), Class.forName(jobject.getAsJsonPrimitive("weightClass").getAsString()));
            return (IWeight) gson.fromJson(jobject.toString(), Class.forName(jobject.getAsJsonPrimitive("weightClass").getAsString()));
        } catch (ClassNotFoundException e) {
            logger.error("Cannot deserialize signalChain " + jobject.getAsJsonObject("weight").toString() + " for class " + jobject.getAsJsonPrimitive("weightClass").getAsString(), e);
            throw new JSONParsingException(e.getMessage());
        }
    }
}
