package com.rakovpublic.jneuropallium.worker.neuron.impl;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.rakovpublic.jneuropallium.worker.neuron.IWeight;

import java.io.IOException;

public class WeightDeserializer extends StdDeserializer<IWeight> {
    public WeightDeserializer(){
        this(null);
    }
    protected WeightDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public IWeight deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
        ObjectMapper mapper= new ObjectMapper();
        JsonElement jelement = new com.google.gson.JsonParser().parse(jsonParser.readValueAsTree().toString());
        JsonObject jobject = jelement.getAsJsonObject();
        try {
            return (IWeight) mapper.readValue(jobject.getAsJsonObject("weight").toString(),Class.forName(jobject.getAsJsonPrimitive("weightClass").getAsString()));
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }
}
